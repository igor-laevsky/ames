(defproject ames/cdl "0.1.0"
  :description ""
  :url "https://github.com/igor-laevsky/ames"
  :license {}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-time "0.15.0"]
                 [json-path "1.0.1"]]
  :profiles {:dev
             {:dependencies [[org.clojure/test.check "0.10.0-alpha3"]]
              :source-paths ["dev"]
              :resource-paths ["test/resource"]}}
)
