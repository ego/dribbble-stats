(ns api-octopus.dribbble.windows
  (:require [api-octopus.dribbble.functions :as dfn]))

(defn build-windows []
  [{:window/id          :count-liker-window
    :window/task        :count-liker
    :window/type        :global
    :window/aggregation :onyx.windowing.aggregation/count
    :window/window-key  :event-time}])

(defn build-triggers []
  [{:trigger/window-id  :count-liker-window
    :trigger/refinement :onyx.refinements/accumulating
    :trigger/on         :onyx.triggers/segment
    :trigger/threshold  [1 :elements]
    :trigger/sync       ::dfn/dump-count-liker-window!}])
