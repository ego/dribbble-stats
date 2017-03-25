(ns api-octopus.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as clog]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.core.async :refer [chan <!! thread alts!! timeout]]

            [aero.core :refer [read-config]]
            [mount.core :as mount]
            [com.stuartsierra.component :as component]

            [onyx.extensions :as ext]
            [onyx.log.entry :as entry]
            [onyx.static.validation :as validator]
            [lib-onyx.peer :as peer]
            [onyx.job]
            [onyx.api]
            [onyx.test-helper]
            [onyx.plugin [core-async]]

            [api-octopus.dribbble
             [tasks :as tk]
             [jobs :as dj]
             [functions :as fn]]
            [api-octopus.web.server :as web]))

(defn file-exists?
  "Check both the file system and the resources/ directory
  on the classpath for the existence of a file"
  [file]
  (let [f      (clojure.string/trim file)
        classf (io/resource file)
        relf   (when (.exists (io/as-file f)) (io/as-file f))]
    (or classf relf)))

(defn cli-options []
  [["-c" "--config FILE" "Aero/EDN config file"
    :default (io/resource "config.edn")
    :default-desc "resources/config.edn"
    :parse-fn file-exists?
    :validate
    [identity "File does not exist relative to the workdir or on the classpath"
     read-config "Not a valid Aero or EDN file"]]

   ["-p" "--profile PROFILE" "Profile mode: dev test prod"
    :parse-fn keyword]

   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["Onyx Peer and Job Launcher"
        ""
        "Usage: [options] action [arg]"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  start-peers [npeers]    Start Onyx peers."
        "  submit-job  [job-name]  Submit a registered job to an Onyx cluster."
        "  web-server  Start web server."
        "Usage:"
        "  [lein run] [*.jar] -p dev start-peers 7"
        "  [lein run] [*.jar] --profile prod submit-job dribbble-job"
        "  [lein run] [*.jar]        -p dev submit-job dribbble-job"
        ""]
    (clojure.string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
    (clojure.string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn assert-job-exists [job-name]
  (let [jobs (methods onyx.job/register-job)]
    (when-not (contains? jobs job-name)
      (exit 1 (error-msg (into [(str "There is no job registered under the name " job-name "\n")
                                "Available jobs: "] (keys jobs)))))))

(defn kill-all-jobs [config]
  (let [ch                    (chan 100)
        {:keys [replica env]} (onyx.api/subscribe-to-log config ch)]
    (loop [i 1]
      (when-let [[event _] (alts!! [ch (timeout 500)])]
        (when (= (:fn event) :submit-job)
          (let [job-id (-> event :args :id)
                entry  (entry/create-log-entry :kill-job
                         {:job (validator/coerce-uuid job-id)})]
            (println "Killing job" job-id)
            (ext/write-log-entry (:log env) entry)))

        (recur (inc i))))

    (component/stop env)))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]
         :as   pargs} (parse-opts args (cli-options))

        edn-profile (cond
                      (= :dev  (:profile options)) :default
                      (= :test (:profile options)) :default
                      :else                        (:profile options))
        action      (first arguments)
        argument    (clojure.edn/read-string (second arguments))
        job-name    (if (keyword? argument) argument (str argument))

        {:keys [profile env-config peer-config job-config]
         :as   config} (read-config (:config options) {:profile edn-profile})]

    (cond (:help options)               (exit 0 (usage summary))
          (and (not= action "web-server")
            (not= (count arguments) 2)) (exit 1 (usage summary))
          errors                        (exit 1 (error-msg errors)))

    (case action
      "web-server"
      (do (mount/start)
          (web/run-web-server (:web config)))

      "start-peers"
      (peer/start-peer argument peer-config env-config)

      "submit-job"
      (let [_      (assert-job-exists job-name)
            job-id (:job-id (onyx.api/submit-job
                              peer-config
                              (onyx.job/register-job job-name config)))]

        (println "Successfully submitted job: " job-id)
        (println "Blocking on job completion...")
        (onyx.test-helper/feedback-exception! peer-config job-id)
        (exit 0 "Job completed"))

      "kill-all-jobs"
      (kill-all-jobs env-config)
      (exit 0 "Job killeded"))))
