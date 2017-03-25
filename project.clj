(defproject api-octopus (-> (slurp "VERSION") .trim)
  :description "API-OCTOPUS - onyx project"
  :url ""
  :manifest {"Implementation-Version" ~(-> (slurp "VERSION") .trim)}
  :source-paths #{"src"}
  :resource-paths #{"resources"}
  :license {:name ""
            :url  ""}
  :dependencies [[aero "1.0.1" :exclusions [prismatic/schema]]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.371"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.logging "0.3.1"]

                 [com.stuartsierra/component "0.2.3"]
                 [mount "0.1.10"]

                 [org.onyxplatform/onyx "0.9.11"]
                 [org.onyxplatform/lib-onyx "0.9.7.1"]
                 [org.onyxplatform/onyx-http "0.9.11.0"]
                 [org.onyxplatform/onyx-seq "0.9.11.0"]
                 [org.onyxplatform/onyx-kafka "0.9.11.1"]

                 [ymilky/franzy "0.0.1"]

                 ;; Installed into local repo.
                 ;; [onyx-cassandra "0.5.4"]

                 [clj-http "2.2.0"]
                 [clj-time "0.12.0"]
                 [prismatic/schema "1.1.3"]
                 [cheshire "5.6.3"]
                 [cc.qbits/alia-all "3.1.11"]

                 [compojure "1.5.1"]
                 [http-kit "2.1.18"]
                 [ring/ring-defaults "0.2.1"]
                 [ring-logger "0.7.6"]
                 [hiccup "1.0.5"]

                 [com.cognitect/transit-clj "0.8.290"]
                 [com.cognitect/transit-cljs "0.8.239"]

                 [org.clojure/clojurescript "1.9.293"]
                 [lein-cljsbuild "1.0.6"]

                 [reagent "0.5.0"]
                 [cljsjs/react "0.13.3-0"]
                 [reagent-forms "0.5.1"]
                 [reagent-utils "0.1.4"]]

  :main api-octopus.core

  :plugins [[lein-figwheel "0.5.7"]]

  :cljsbuild {:builds
              [{:id           "dev"
                :source-paths ["src/api_octopus/web"]
                :figwheel     true
                :compiler     {:main       "api-octopus.web.fe"
                               :asset-path "js/out"
                               :output-to  "resources/public/js/main.js"
                               :output-dir "resources/public/js/out"}}]}

  :profiles
  {:uberjar {:aot          [lib-onyx.media-driver api-octopus.core]
             :uberjar-name "api-octopus-standalone.jar"}
   :dev     {:jvm-opts     ["-XX:-OmitStackTraceInFastThrow"]
             :dependencies [[org.clojure/tools.namespace "0.2.11"]
                            [lein-project-version "0.1.0"]
                            [lein-update-dependency "0.1.2"]
                            [lein-pprint "1.1.1"]
                            [lein-set-version "0.4.1"]]
             :plugins      [[com.jakemccrary/lein-test-refresh "0.15.0"]
                            [lein-cljfmt "0.5.5"]]
             :cljfmt       {:indentation?                    false
                            :file-pattern                    #"\.clj[sc]?$"
                            :remove-consecutive-blank-lines? false}
             :source-paths #{"api-octopus"}
             :test-refresh
             {:notify-command
              ["terminal-notifier" "-title" "Tests" "-message"]
              :changes-only true}}})
