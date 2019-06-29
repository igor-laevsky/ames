(defproject ames/db-api "0.1.0"
  :description ""
  :url "https://github.com/igor-laevsky/ames"
  :license {}
  :main db-api.main
  :aot :all
  :repl-options {:init-ns user}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.json "0.2.6"]
                 [com.stuartsierra/component "0.4.0"]

                 ; Logging
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [org.slf4j/slf4j-api "1.7.25"]
                 [org.clojure/tools.logging "0.3.1"]

                 ; Elasticsearch
                 [cc.qbits/spandex "0.7.0-beta3"]

                 ; Pedestal
                 [io.pedestal/pedestal.service "0.5.5"]
                 [io.pedestal/pedestal.jetty "0.5.5"]

                 ; Other components of this project
                 [ames/cdl "0.1.0"]]
  :profiles {:dev
             {:dependencies [[org.clojure/test.check "0.10.0-alpha3"]
                             [orchestra "2018.12.06-2"]
                             [org.clojure/tools.namespace "0.2.11"]
                             [com.stuartsierra/component.repl "0.2.0"]]
              :source-paths ["dev"]
              :resource-paths ["test/resources"]}
             :test [:dev {:resource-paths ["test/resources/logging"]}]}
)
