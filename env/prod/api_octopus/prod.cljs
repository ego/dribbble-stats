(ns prod.api-octopus.prod
  (:require [api-octopus.web.fe :as fe]))

(println "prod!")
(fe/init!)
