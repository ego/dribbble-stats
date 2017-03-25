(ns api-octopus.web.handlers
  (:require [taoensso.timbre :as logs]

            [api-octopus.dribbble.functions :as fn]
            [api-octopus.web
             [kafka :as kf]
             [layout :as la]]))

(def header {"Content-Type" "text/html; charset=utf-8"})

(defn index [request]
  {:headers header
   :status  200
   :body    (la/application "Index page" (la/index))})

(defn index-post [request]
  (logs/infof "index page params %s" (:params request))
  (let [data (kf/producer-send-msg kf/producer-connector
               (get-in request [:params :username]))]
    {:headers header
     :status  200
     :body    (la/application "Post result page" (la/index-post data))}))

(defn index-top10 [params]
  (logs/infof "index-top10 page req q-params %s" params)
  (let [data (select-keys params [:username :message_id])
        rows (fn/get-top10-likers data)
        body (la/application "Top 10 likers page"
               (la/index-post (assoc data :rows rows)))]
    {:headers header
     :status  200
     :body    body}))
