(ns crawler.main
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]

            [crawler.network :as network]
            [crawler.extractor :as extractor]
            [crawler.saver :as saver]
            [crawler.etl :as etl]))

(def ^:const parallelism 2)

(def creds (-> "creds.edn" io/resource io/reader slurp read-string))

(def locations [1 2 3 4 5 7 8 10 11 12 13 14 17 18 19 23 30 31 32])

(defn create-system []
  (component/system-map
    :network (network/make-network {:num-threads parallelism
                                    :rate 2})
    :saver (saver/make-elastic-saver {:url "http://127.0.0.1:9200"
                                      :index "vnok"})
    :etl (component/using
           (etl/make-login-etl creds)
           [:network :saver])))

(defn -main [& args]
  (let [system (component/start-system (create-system))]
    (try
      (doseq [loc locations]
        (log/info "Parsing center" loc)
        (etl/switch-center! (:etl system) loc)
        (etl/parse-center! (:etl system) :num-parser-threads parallelism))
      (finally
        (component/stop-system system)))))
