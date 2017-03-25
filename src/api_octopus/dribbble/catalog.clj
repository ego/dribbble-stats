(ns api-octopus.dribbble.catalog
  (:require [onyx.plugin.http-output]
            [onyx.plugin.kafka]
            [api-octopus.dribbble.functions :as dfn]
            [onyx.plugin.http-output :as http-output]))

(defn build-catalog [batch-size batch-timeout]
  [{:onyx/name                  :in
    :onyx/plugin                :onyx.plugin.kafka/read-messages
    :onyx/type                  :input
    :onyx/medium                :kafka
    :kafka/topic                "onyx-api-octopus"
    :kafka/group-id             "onyx-consumer"
    :kafka/receive-buffer-bytes 65536
    :kafka/zookeeper            "127.0.0.1:2181"
    :kafka/offset-reset         :latest
    :kafka/force-reset?         false
    :kafka/deserializer-fn      :onyx.tasks.kafka/deserialize-message-edn
    :kafka/wrap-with-metadata?  false
    :onyx/batch-timeout         50
    :onyx/min-peers             1
    :onyx/max-peers             1
    :onyx/batch-size            100
    :onyx/doc                   "Reads messages from a Kafka topic"}])
