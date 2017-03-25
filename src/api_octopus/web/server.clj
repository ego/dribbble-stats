(ns api-octopus.web.server
  (:require [taoensso.timbre :as logs]
            [mount.core :refer [defstate]]

            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as handler]

            [ring.middleware.reload :as reload]
            [ring.logger :as logger]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]

            [cognitect.transit :as t]

            [org.httpkit.server :refer [run-server send! with-channel
                                        on-close on-receive close]]
            [org.httpkit.timer :refer [schedule-task]]

            [api-octopus.dribbble.functions :as fns]
            [api-octopus.web
             [handlers :as ha]
             [kafka :as kf]])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defonce channels (atom #{}))

(def out (ByteArrayOutputStream. 4096))
(def writer (t/writer out :json))

(defn- to-bytes [s]
  (cond
    (string? s)     (.getBytes s "UTF-8")
    (sequential? s) (into-array Byte/TYPE (.getBytes s "UTF-8"))
    :else s))

(defn connect! [channel]
  (logs/info "channel open")
  (swap! channels conj channel))

(defn disconnect! [channel status]
  (logs/info "channel closed:" status)
  (swap! channels #(remove #{channel} %)))

(defn notify-clients [msg]
  (logs/info "notify-clients...")
  (doseq [channel @channels]
    (send! channel msg)))

(defn ws-handler [request]
  (logs/info "ws-handler...")
  (with-channel request channel
    (connect! channel)
    (on-close channel (partial disconnect! channel))
    (on-receive channel #(notify-clients %))))

(defn stream-handler [request]
  (println "stream-handler...")
  (with-channel request channel
    (println "stream-handler with-channel...")

    (on-close channel
      (fn [status] (println "stream-handler channel closed " status)))

    (loop [id 0]
      (println "stream-handler loop ...#" id)
      (when-not (get @fns/initial-username :job-completed)
        (let [data (fns/get-top10-likers
                     {:username   (or "shapeux" (:username @fns/initial-username))
                      :message_id (or "db193e02-a655-473d-b56e-ce41288e873d"
                                    (fns/message-id->uuid @fns/initial-username))})
              _    (t/write writer (assoc @fns/initial-username :rows data))]
          (println "stream-handler loop ...#" id " data " data)
          (Thread/sleep 5000)
          (send! channel (ByteArrayInputStream. (.toByteArray out))))
        (recur (inc id))))

    (println "stream-handler closing chanel")
    (schedule-task 10000 (close channel))))

(defroutes application
  (GET "/" request (ha/index request))
  (POST "/username" request (ha/index-post request))
  (GET "/top10" {:keys [params]} (ha/index-top10 params))
  (GET "/ws" request (ws-handler request))
  (GET "/stream" request (stream-handler request))
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>"))

(defn run-web-server [web-config]
  (let [port-map (select-keys web-config [:port])]
    (logs/infof "Web server start at http://localhost:%s" (:port port-map))
    (run-server
      (logger/wrap-with-logger
        (reload/wrap-reload
          (handler/site
            (wrap-defaults (handler/api application) site-defaults))))
      port-map)))

(defonce web-server (atom nil))

(defn stop-web-server []
  (when-not (nil? @web-server)
    (@web-server :timeout 100)
    (reset! web-server nil)))

(defstate web-app
  :start run-web-server
  :stop (stop-web-server))
