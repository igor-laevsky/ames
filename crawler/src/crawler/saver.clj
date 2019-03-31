(ns crawler.saver
  (:require [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.data.json :as js]
            [com.stuartsierra.component :as component]
            [qbits.spandex :as es]

            [cdl.core :as cdl])
  (:import (java.io Writer)
           (org.elasticsearch.client RestClient)))

;;; Saves parsed exps into a persistent storage.
;;;

(defprotocol Saver
  (-save [_ exp]))

(deftype NoOpSaver []
  Saver
  (-save [_ exp] exp))

(defn make-no-op-saver [] (->NoOpSaver))

(defrecord FileSaver [^Writer file-writer params]
  component/Lifecycle
  (start [this]
    (log/info "Starting file based saver" params)
    (-> this
        (assoc :file-writer (io/writer (:file-name params) :encoding "UTF-8"))))
  (stop [this]
    (log/info "Stopping file based saver")
    (.close file-writer)
    this)

  Saver
  (-save [this exp]
    (js/write exp file-writer :escape-unicode false)
    (.write file-writer (System/getProperty "line.separator"))
    ; this simplifies testing
    (.flush file-writer)))

;; params must contain ':file-name' key.
(defn make-file-saver [params] (map->FileSaver {:params params}))

;; Creates elastic search index capable of storing exps from the ElasticSaver
(defn create-es-index [{:keys [es-client params] :as this}]
  (es/request es-client
              {:method :put
               :url (str (:index params) "?include_type_name=false")
               :body
               {:mappings
                {
                  "dynamic_templates"
                  [
                   ; Exact match for everything
                   {
                    "strings_as_keywords"
                    {
                     "match_mapping_type" "string",
                     "mapping" { "type" "keyword" }
                    }
                    }
                   ]
                  "date_detection" false
                 }
                :settings
                {
                 "index"
                 {
                  "number_of_shards" 1
                  }
                 }}
               })
  this)

(defrecord ElasticSaver [^RestClient es-client params]
  component/Lifecycle
  (start [this]
    (log/info "Starting elastic saver" params)
    (-> this
        (assoc :es-client (es/client {:hosts [(:url params)]}))
        (create-es-index)))
  (stop [this]
    (log/info "Stopping elastic saver")
    (.close es-client))

  Saver
  (-save [this exp]
    (es/request es-client
                {:method :post
                 :url (str (:index params) "/_doc")
                 :body exp})))

;; 'params' should contain:
;;   :url - elastic search url
;;   :index - name for the elasticsearch index
(defn make-elastic-saver [params] (map->ElasticSaver {:params params}))

;; Receives parsed exp conforming to the cdl spec and synchronously saves it
;; according with the saver policy.
(defn save [saver exp]
  (-save saver exp))

(s/fdef save
  :args (s/cat :saver (complement nil?) :input (s/spec ::cdl/exp)))
