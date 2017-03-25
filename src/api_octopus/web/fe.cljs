(ns api-octopus.web.fe
  (:require [cognitect.transit :as t]
            [reagent.core :as reagent :refer [atom]]
            [clojure.string :as s]))

(defonce ws-chan (atom nil))
(def json-reader (t/reader :json))
(def json-writer (t/writer :json))

(defn receive-transit-msg!
  [update-fn]
  (fn [msg]
    (update-fn msg)))

(defn send-transit-msg!
  [msg]
  (if @ws-chan
    (.send @ws-chan (t/write json-writer msg))
    (throw (js/Error. "Websocket is not available!"))))

(defn make-websocket! [url receive-handler]
  (println "attempting to connect websocket")
  (if-let [chan (js/WebSocket. url)]
    (do
      (println ".-onmessage")
      (set! (.-onmessage chan) (receive-transit-msg! receive-handler))
      (reset! ws-chan chan)
      (println "Websocket connection established with: " url))
    (throw (js/Error. "Websocket connection failed!"))))

(defn update-messages! [data]
  (println "update-messages!" data)
  (println "data: " (->> data .-data))
  (println "data!! " (t/read json-reader (->> data .-data))))

(defn init! []
  (.log js/console "init!")
  (make-websocket! (str "ws://" (.-host js/location) "/stream") update-messages!))
