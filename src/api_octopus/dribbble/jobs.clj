(ns api-octopus.dribbble.jobs
  (:require [onyx.job :refer [add-task register-job]]
            [onyx.tasks.core-async :as core-async-task]

            [api-octopus.common
             [file-input :as fp]]
            [api-octopus.dribbble
             [workflow :as wf]
             [catalog :as cat]
             [lifecycles :as lc]
             [tasks :as tk]
             [functions :as dfn]
             [windows :as w]]))

(defn dribbble-job
  [mode {size :onyx/batch-size timeout :onyx/batch-timeout :as batch-settings}]
  (let [dev?     (or (= :dev mode) (= :default mode) (= :test mode))
        prod?    (= :prod mode)
        base-job {:workflow        (wf/build-workflow)
                  :catalog         (cat/build-catalog size timeout)
                  :lifecycles      (lc/build-lifecycles)
                  :windows         (w/build-windows)
                  :triggers        (w/build-triggers)
                  :flow-conditions (wf/build-flow-conditions)
                  :task-scheduler  :onyx.task-scheduler/balanced}]
    (cond-> base-job
      dev? (add-task (fp/input-task :in
                       {:filename "env/dev/api_octopus/dev_inputs/input.json"}))

      prod? (add-task (tk/finder :find-followers
                        (merge {:onyx/fn ::dfn/find-followers} batch-settings)))

      true (add-task (tk/finder :find-shots
                       (merge {:onyx/fn ::dfn/find-shots} batch-settings)))

      true (add-task (tk/finder :find-likes
                       (merge {:onyx/fn ::dfn/find-likes} batch-settings)))

      true (add-task (tk/counter :count-liker batch-settings))

      dev?  (add-task (core-async-task/output :out batch-settings))
      prod? (add-task (tk/leaf-out :out batch-settings)))))

(defmethod register-job "dribbble-job"
  [job-name config]
  (let [mode           (:profile config)
        batch-settings {:onyx/batch-size 1 :onyx/batch-timeout 1000}]
    (dribbble-job mode (merge (:job-config config) batch-settings))))
