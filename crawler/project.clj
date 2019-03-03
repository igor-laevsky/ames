 (defproject ames/crawler "0.1.0"
  :description ""
  :url "https://github.com/igor-laevsky/ames"
  :license {}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/spec.alpha "0.2.176"]
                 [org.clojure/core.async "0.4.490"]
                 [clj-time "0.15.0"]
                 [clj-http "3.9.1"]
                 [com.stuartsierra/component "0.4.0"]]
  :profiles {:dev
             {:dependencies [[org.clojure/test.check "0.10.0-alpha3"]
                             [orchestra "2018.12.06-2"]
                             [clj-http-fake "1.0.3"]
                             [org.clojure/tools.namespace "0.2.11"]
                             [com.stuartsierra/component.repl "0.2.0"]]
              :source-paths ["dev"]
              :resource-paths ["test/resource"]}}
)