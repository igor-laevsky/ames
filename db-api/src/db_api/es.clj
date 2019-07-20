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
;;
(defn search [{:keys [params es-client]} query from size]
  (->
    (spandex/request es-client
      {:method :get
       :url    (str (:index params) "/_search?q=" query "&from=" from "&size=" size)})
    :body))

;; Counts exps groupped by the location and verified status.
;;
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

;; Filters exps from the given location and groups them by patient.
;;
(defn list-patients [{:keys [params es-client] :as es} loc]
  (->
    (spandex/request es-client
                     {:method :get
                      :url    (str (:index params) "/_search")
                      :body   (->
                                "{
                                    \"size\": 0,
                                    \"query\": {
                                      \"match\": {
                                        \"location\": \"%s\"
                                      }
                                    },
                                    \"aggs\" : {
                                        \"patients\" : {
                                            \"terms\" : {
                                              \"script\" : {
                                                \"source\": \"doc['patient.name'].value + ' ' + doc['patient.rand-num'].value\",
                                                \"lang\": \"painless\"
                                              },
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
                                }"
                                (format loc)
                                spandex/->Raw)})
    :body))

;; List all exps for a given patient.
;;
(defn list-exps [{:keys [params es-client] :as es} name from size]
  (search es (str "patient.name:" name) from size))

;; List all visits for a given location and patient.
;;
(defn list-visits [{:keys [params es-client] :as es} loc patient]
  (->
    (spandex/request es-client
                     {:method :get
                      :url    (str (:index params) "/_search")
                      :body   (->
                                "{
                                  \"size\": 0,
                                  \"query\": {
                                    \"bool\": {
                                      \"must\": [
                                        {
                                          \"match\": {
                                            \"location\": \"%s\"
                                          }
                                        },
                                        {
                                          \"match\": {
                                            \"patient.name\": \"%s\"
                                          }
                                        }
                                      ]
                                    }
                                  },
                                  \"aggs\": {
                                    \"visits\": {
                                      \"terms\": {
                                        \"script\" : {
                                          \"source\": \"doc['visit'].value + ' ' + doc['group'].value\",
                                          \"lang\": \"painless\"
                                        },
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
                                }"
                                (format loc patient)
                                spandex/->Raw)})
    :body))
