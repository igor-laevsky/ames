(ns db-api.es
  (:require
    [clojure.tools.logging :as log]
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

(defn- index [es] (get-in es [:params :index]))

;; Fulltext search. 'request' should be a string representing "query string query"
;; in elasticsearch terms.
(defn search [es request]
  (->
    (spandex/request
      (:es-client es)
      {:method :get
       :url    (str (index es) "/_search?q=" request)})
    :body))