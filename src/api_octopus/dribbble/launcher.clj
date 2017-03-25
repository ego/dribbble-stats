(ns api-octopus.dribbble.launcher
  (:gen-class)
  (:require [aero.core :refer [read-config]]
            [clojure.java.io :as io]
            [lib-onyx.peer :as peer]
            [onyx.job]
            [onyx.api]
            [onyx.test-helper]))

(defn assert-job-exists [job-name]
  (let [jobs (methods onyx.job/register-job)]
    (contains? jobs job-name)))

(defn onyx-main
  "{:config-edn  config.edn
    :profile-edn default
    :action      start-peers-submit-job
    :argument    7
    :job-name    dribbble-job}"

  [{:keys [config-edn profile-edn action argument job-name]
    :or   {config-edn "config.edn" profile-edn "default"}}]

  (let [{:keys [profile env-config peer-config job-config]
         :as   config} (read-config (io/resource config-edn)
                         {:profile (keyword profile-edn)})]

    (case action
      "start-peers-submit-job"
      (when-let [_ (assert-job-exists job-name)]
        (let [env        (onyx.api/start-env env-config)
              peer-group (onyx.api/start-peer-group peer-config)
              peers      (onyx.api/start-peers argument peer-group)
              job-id     (:job-id (onyx.api/submit-job
                                    peer-config
                                    (onyx.job/register-job job-name config)))]
          ;; awayting
          (onyx.api/await-job-completion peer-config job-id)
          (doseq [v-peer peers]
            (onyx.api/shutdown-peer v-peer))
          (onyx.api/shutdown-peer-group peer-group)
          (onyx.api/shutdown-env env))))))
