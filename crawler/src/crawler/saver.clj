(ns crawler.saver
  (:require [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.data.json :as js]
            [com.stuartsierra.component :as component]

            [cdl.core :as cdl])
  (:import (java.io Writer)))

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

;; Receives parsed exp conforming to the cdl spec and synchronously saves it
;; according with the saver policy.
(defn save [saver exp]
  (-save saver exp))

(s/fdef save
  :args (s/cat :saver (complement nil?) :input (s/spec ::cdl/exp)))
