(ns api-octopus.dribbble.workflow)

(defn build-workflow []
  [[:in                :find-followers]
   [:find-followers    :find-shots]
   [:find-shots        :find-likes]
   [:find-likes        :count-liker]
   [:count-liker       :out]])

(defn build-flow-conditions []
  [])
