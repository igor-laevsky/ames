(defproject ames "0.1.0"
  :description ""
  :url "https://github.com/igor-laevsky/ames"
  :license {}
  :plugins [[lein-sub "0.3.0"]]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [ames/cdl "0.1.0"]
                 [ames/crawler "0.1.0"]
                 [ames/db-api "0.1.0"]]
  :sub ["cdl" "crawler" "db-api"])
