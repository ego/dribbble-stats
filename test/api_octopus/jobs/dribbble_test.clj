(ns api-octopus.jobs.dribbble-test
  (:require [aero.core :refer [read-config]]
            [clojure.core.async :refer [>!!]]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [onyx api
             [test-helper :refer [with-test-env]]]
            [onyx.plugin.core-async :refer [get-core-async-channels
                                            take-segments!]]
            [api-octopus.dribbble
             [jobs :as dj]
             [tasks :as tk]]
            onyx.tasks.core-async))

(def min-peers-count 7)

(deftest dribbble-test
  (testing "Testing dribbble job"
    (let [mode             :dev
          {:keys [env-config peer-config job-config]}
          (read-config (io/resource "config.edn"))
          job              (dj/dribbble-job mode
                             (merge job-config
                               {:onyx/batch-size    10
                                :onyx/batch-timeout 1000}))
          {:keys [in out]} (get-core-async-channels job)]
      (with-test-env [test-env [min-peers-count env-config peer-config]]
        (onyx.test-helper/validate-enough-peers! test-env job)
        (let [job-id (:job-id (onyx.api/submit-job peer-config job))
              result (take-segments! out)]
          (onyx.api/await-job-completion peer-config job-id))))))
