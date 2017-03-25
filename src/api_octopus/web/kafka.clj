(ns api-octopus.web.kafka
  (:require [taoensso.timbre :as logs]
            [clojure.java.io :as io]
            [aero.core :refer [read-config]]

            [mount.core :refer [defstate]]

            [franzy.serialization.serializers :as serializers]
            [franzy.clients.producer.client :as producer]
            [franzy.clients.producer.defaults :as pd]
            [franzy.clients.producer.protocols :refer :all]))

(defn make-cong []
  (read-config (io/resource "config.edn") {:profile :prod}))

(def kafka-input (atom {}))

(defn create-producer [config]
  (let [pc               {:bootstrap.servers (:kafka-bootstrap.servers config)
                          :group.id          "api-octopus-web"}
        key-serializer   (serializers/keyword-serializer)
        value-serializer (serializers/edn-serializer)]
    (producer/make-producer pc key-serializer value-serializer)))

(defstate producer-connector :start (create-producer (make-cong)))

(defn producer-send-msg [c-producer value]
  (let [topic     "onyx-api-octopus"
        partition 0]
    (let [data     {:message_id (str (java.util.UUID/randomUUID))
                    :username   value}
          _        (reset! kafka-input (assoc data :job-completed false))
          send-msg (send-async!  c-producer
                     {:topic     topic
                      :partition partition
                      :key       :data
                      :value     data})]
      (logs/infof "Async send message to kafka %s" data)
      data)))
