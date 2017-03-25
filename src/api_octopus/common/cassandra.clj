(ns api-octopus.common.cassandra
  (:require [taoensso.timbre :as logs]
            [qbits.alia :as alia]))

(defn get-conn [spec]
  (let [cluster (alia/cluster {:contact-points (:hosts spec)})
        conn    (alia/connect cluster (:keyspace spec))]
    {:cluster cluster :conn conn}))

(defn shutdown [{:keys [cluster conn]}]
  (when (and cluster conn)
    (alia/shutdown conn)
    (alia/shutdown cluster)))

(defn q
  ([conn query]
   (alia/execute conn query))
  ([conn query args]
   (alia/execute conn query {:values args})))

(defn one
  ([conn query]
   (first (q conn query)))
  ([conn query args]
   (first (q conn query args))))
