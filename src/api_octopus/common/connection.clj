(ns api-octopus.common.connection
  (:require [taoensso.timbre :as log]
            [api-octopus.common.cassandra :as cassandra]))

(defn inject-cassandra [event lifecycle]
  (log/debug "Opening cassandra connection"
    (-> event :onyx.core/task-map
      :api-octopus/cassandra-spec))

  (let [params (-> event :onyx.core/params)
        conn   (-> event
                 :onyx.core/task-map
                 :api-octopus/cassandra-spec
                 cassandra/get-conn)]
    {:onyx.core/params (merge params {:cassandra conn})}))

(defn close-cassandra [event lifecycle]
  (log/debug "Close cassandra connection")
  (when-let [conn (-> event :onyx.core/task-map
                    :onyx.core/params
                    first
                    :cassandra)]
    (cassandra/shutdown conn))
  {})
