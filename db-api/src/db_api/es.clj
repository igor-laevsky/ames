(ns db-api.es
  (:require
    [clojure.tools.logging :as log]
    [clojure.data.json :as json]
    [com.stuartsierra.component :as component]
    [qbits.spandex :as spandex]))

(defrecord ElasticSearch [params es-client]
  component/Lifecycle
  (start [this]
    (if es-client
      this
      (assoc this :es-client (spandex/client {:hosts [(:host params)]}))))

  (stop [this]
    (when es-client
      (spandex/close! es-client))
    (assoc this :es-client nil)))

(defn make-es [params]
  (map->ElasticSearch {:params params}))

;; Fulltext search. 'request' should be a string representing "query string query"
;; in elasticsearch terms.
(defn search [{:keys [params es-client]} query from size]
  (->
    (spandex/request es-client
      {:method :get
       :url    (str (:index params) "/_search?q=" query "&from=" from "&size=" size)})
    :body))

;; Counts exps groupped by the location and verified status.
(defn list-locations [{:keys [params es-client] :as es}]
  (->
    (spandex/request es-client
                     {:method :get
                      :url    (str (:index params) "/_search")
                      :body   (spandex/->Raw
                                "{
                                  \"size\": 0,
                                  \"aggs\": {
                                    \"locations\": {
                                      \"terms\": {
                                        \"field\": \"location\",
                                        \"size\": 10000
                                      },
                                      \"aggs\": {
                                        \"verified\": {
                                          \"terms\": {
                                            \"field\": \"verified\",
                                            \"size\": 10000
                                          }
                                        }
                                      }
                                    }
                                  }
                                }")})
    :body))
