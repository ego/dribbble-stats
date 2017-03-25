(ns api-octopus.common.file-input
  (:require [schema.core :as s]
            [cheshire.core :as json]
            [onyx.schema :as os]
            [onyx.plugin.seq :as seq]))

(defn inject-in-reader [event lifecycle]
  (let [filename (:filename (:onyx.core/task-map event))]
    {:seq/seq (json/parse-string (slurp filename) true)}))

(def in-seq-calls
  {:lifecycle/before-task-start inject-in-reader})

(def SeqInputTask
  {(s/required-key :filename) s/Str})

(s/defn input-task
  ([task-name :- s/Keyword opts]
   {:task   {:task-map
             (merge {:onyx/name       task-name
                     :onyx/plugin     ::seq/input
                     :onyx/type       :input
                     :onyx/medium     :seq
                     :onyx/max-peers  1
                     :onyx/batch-size 1
                     :onyx/doc        "Reads segments from json"}
               opts)
             :lifecycles [{:lifecycle/task  task-name
                           :lifecycle/calls ::in-seq-calls}
                          {:lifecycle/task  task-name
                           :lifecycle/calls ::seq/reader-calls}]}
    :schema {:task-map   SeqInputTask
             :lifecycles [os/Lifecycle]}})

  ([task-name :- s/Keyword
    filename :- s/Str
    task-opts]
   (input-task task-name (merge {:filename filename} task-opts))))
