(ns user
  (:require [clojure.tools.namespace.repl :as nsrepl]
            [clojure.test :as test]))

(nsrepl/set-refresh-dirs "src" "test")

(def refresh nsrepl/refresh)
(def run-tests test/run-tests)
(def run-all-tests test/run-all-tests)
