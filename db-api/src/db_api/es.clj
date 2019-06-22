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
