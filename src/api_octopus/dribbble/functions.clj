(ns api-octopus.dribbble.functions
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clojure.core.async :as a :refer
             [>! <! >!! <!! put! take! go go-loop chan close! thread]]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :as c]
            [clj-http.client :as client]
            [taoensso.timbre :as logs]
            [qbits.alia :as alia]

            [clojure.java.io :as io]
            [aero.core :refer [read-config]]

            [api-octopus.common.cassandra :as cassa]
            [api-octopus.dribbble.dal :as dal]
            [api-octopus.utils.url-match :as utils]))

(defn dribbble-pattern []
  (utils/pattern "host(api.dribbble.com);
                  queryparam(access_token=?token);
                  queryparam(page=?page);"))

(def url-user "https://api.dribbble.com/v1/users/")

(defn url-user-followers [username]
  (str url-user username "/followers"))

(defn url-user-shots [username]
  (str url-user username "/shots"))

(defn get-url [url access_token page]
  (logs/info "requests: " (str "GET " url " page=" page " per_page=" 100))
  (let [res (client/get url {:query-params     {:access_token access_token
                                                :page         page
                                                :per_page     100}
                             :as               :json :insecure? true
                             :throw-exceptions false})]
    (println res)
    res))

;; Rate Limiting:
;; 60 requests per minute
;; 1440 requests per day


(defn lock-token [token]
  (let [count-call (-> token :count_call inc)]
    (cond
      (> count-call 1440)
      (dal/lock-token! token (c/to-sql-time (t/plus (t/now) (t/days 1))))
      :else
      (dal/lock-token! token (c/to-sql-time (t/plus (t/now) (t/seconds 1)))))))

(defn parse-next-page [resp]
  (some->> (-> resp :links :next :href)
    (utils/recognize (dribbble-pattern))
    flatten
    (apply hash-map)
    :page))

(defn parse-followers [resp]
  (into [] (map #(get-in % [:follower :username]) (:body resp))))

(defn do-followers [username followers]
  (for [followers-nested followers
        follower         (doall followers-nested)]
    {:username username :follower follower}))

(defn parse-shots [resp]
  (into [] (map #(get-in % [:likes_url]) (:body resp))))

(defn do-shots [username shots]
  (for [shots-nested shots
        shot         (doall shots-nested)]
    {:username username :shot shot}))

(defn parse-likes [resp]
  (into [] (map #(get-in % [:user :username]) (:body resp))))

(defn do-likes [username likes]
  (for [likes-nested likes
        liker        (doall likes-nested)]
    {:username   username
     :liker      liker
     :event-time (System/currentTimeMillis)
     :n          (str liker "-" (java.util.UUID/randomUUID))}))

;; TODO: create onyx-plugin for it.
(defn get-user-api-data
  [{:keys [username] :as params} conn url do-fn parse-fn]
  (loop [iter    0
         page    1
         collseq []]
    (logs/info "iter = " iter " page " page)
    (if (< (:count @(future (cassa/one conn dal/get-valid-count-call-cql))) 1)
      (do-fn username collseq)
      (let [token (future (cassa/one conn dal/get-valid-token-cql))
            code  (get @token :code)]
        (if code
          (let [_         (future (cassa/q conn (lock-token @token)))
                resp      (<!! (thread (get-url url code page)))
                next-page (parse-next-page resp)]
            (when-not resp
              (logs/info "No response!"))
            (if (not page)
              (do-fn username collseq)
              (recur (inc iter) next-page (conj collseq (parse-fn resp)))))
          (recur (inc iter) page collseq))))))


;;;;; Destructuring functions ;;;;;

(def initial-username (atom {:message_id    0
                             :username      ""
                             :job-completed false}))

(defn find-followers
  [params {{:keys [conn] :as cassandra} :cassandra} segment]
  (logs/info "Run function find-followers..." "segment " segment)
  (swap! initial-username assoc :username (get segment :username)
    :message_id (get segment :message_id))
  (logs/info "Atom: " @initial-username)
  (get-user-api-data params conn
    (url-user-followers (get segment :username segment))
    do-followers
    parse-followers))

(defn find-shots
  [params {{:keys [conn] :as cassandra} :cassandra}
   {:keys [username follower] :as segment}]
  (logs/info "Run function find-shots...")
  (get-user-api-data {:username username}
    conn (url-user-shots follower) do-shots parse-shots))

(defn find-likes
  [params {{:keys [conn] :as cassandra} :cassandra}
   {:keys [username shot] :as segment}]
  (logs/info "Run function find-likes...")
  (get-user-api-data {:username username}
    conn shot do-likes parse-likes))

(defn count-liker
  [{{:keys [conn] :as cassandra} :cassandra} segment]
  (logs/info "Run function count-liker...")
  segment)

(defn message-id->uuid [data]
  (-> data :message_id java.util.UUID/fromString))

(defn dump-count-liker-window!
  "Dump data to cassandra"
  [event-map window trigger {:keys [group-key event-type segment]
                             :as   state-event} state]
  (logs/info "Run function dump-count-liker-window!...")
  (if-let [ca-conn (-> event-map :onyx.core/params first :cassandra :conn)]
    (if-not (= :job-completed event-type)
      (let [prepared (alia/prepare ca-conn dal/insert-liker-cql)
            values   {:values {:message_id  (message-id->uuid @initial-username)
                               :username    (:username @initial-username)
                               :liker_count (int state)
                               :likername   group-key}}
            _        (alia/execute ca-conn prepared values)])
      (swap! initial-username :job-completed true))))

(defn reset-loked-token! [conn]
  (let [codes (future
                (alia/execute conn
                  (alia/prepare conn dal/select-for-reset-token-cql)
                  {:values
                   {:day-before (c/to-sql-time (t/minus (t/now) (t/days 1)))}}))
        _     (future
                (alia/execute conn
                  (alia/prepare conn dal/reset-valid-token-cql)
                  {:values {:codes codes}}))]))

;; TODO: mount cassandra
(defn get-top10-likers [{:keys [username message_id] :as data}]
  (logs/infof "get-top10-likers %s" data)
  (let [config-edn (read-config (io/resource "config.edn") {:profile :prod})
        ca-conn    (-> config-edn :job-config :api-octopus/cassandra-spec
                     cassa/get-conn :conn)
        rows       (alia/execute ca-conn
                     (alia/prepare ca-conn dal/top-user-likers-mv-cql)
                     {:values {:username   username
                               :message_id (message-id->uuid data)}})]
    (cassa/shutdown ca-conn)
    rows))
