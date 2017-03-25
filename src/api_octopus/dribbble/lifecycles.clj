(ns api-octopus.dribbble.lifecycles
  (:require [taoensso.timbre :as log]
            [api-octopus.dribbble.functions :as dfn]
            [api-octopus.dribbble.tasks :as tk]
            [api-octopus.common.connection :as cassa]
            [api-octopus.common.cassandra :as cassandra]))

(defn build-lifecycles []
  [{:lifecycle/task  :in
    :lifecycle/calls :onyx.plugin.kafka/read-messages-calls}])
