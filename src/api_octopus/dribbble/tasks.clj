(ns api-octopus.dribbble.tasks
  (:require [schema.core :as s]
            [api-octopus.dribbble.functions :as dfn]
            [api-octopus.common.connection :as cassa]))

(def FindTask
  {(s/optional-key ::finder-params) s/Any})

(def cassandra-calls
  {:lifecycle/before-task-start cassa/inject-cassandra
   :lifecycle/after-task-stop   cassa/close-cassandra})

(s/defn finder
  ([task-name :- s/Keyword task-opts]
   {:task   {:task-map
             (merge {:onyx/name   task-name
                     :onyx/type   :function
                     :onyx/params [::finder-params]}
               task-opts)
             :lifecycles [{:lifecycle/task  task-name
                           :lifecycle/calls ::cassandra-calls}]}
    :schema {:task-map FindTask}})

  ([task-name :- s/Keyword
    user-map :- s/Any
    task-opts]
   (finder task-name (merge {::finder-params user-map} task-opts))))

(s/defn find-shots
  ([task-name :- s/Keyword task-opts]
   {:task {:task-map
           (merge {:onyx/name task-name
                   :onyx/type :function
                   :onyx/fn   ::dfn/find-shots}
             task-opts)
           :lifecycles [{:lifecycle/task  task-name
                         :lifecycle/calls ::cassandra-calls}]}}))

(s/defn find-likes
  ([task-name :- s/Keyword task-opts]
   {:task {:task-map
           (merge {:onyx/name task-name
                   :onyx/type :function
                   :onyx/fn   ::dfn/find-likes}
             task-opts)
           :lifecycles [{:lifecycle/task  task-name
                         :lifecycle/calls ::cassandra-calls}]}}))

(s/defn counter
  ([task-name :- s/Keyword task-opts]
   {:task {:task-map
           (merge {:onyx/name           task-name
                   :onyx/type           :function
                   :onyx/fn             ::dfn/count-liker
                   :onyx/group-by-key   :liker
                   :onyx/uniqueness-key :n
                   :onyx/flux-policy    :kill
                   :onyx/min-peers      1}
             task-opts)
           :lifecycles [{:lifecycle/task  task-name
                         :lifecycle/calls ::cassandra-calls}]}}))

(s/defn leaf-out
  ([task-name :- s/Keyword task-opts]
   {:task {:task-map
           (merge {:onyx/name   task-name
                   :onyx/fn     :clojure.core/identity
                   :onyx/plugin :onyx.peer.function/function
                   :onyx/medium :function
                   :onyx/type   :output}
             task-opts)}}))
