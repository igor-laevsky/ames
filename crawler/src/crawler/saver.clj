(ns crawler.saver
  (:require [com.stuartsierra.component :as component]
            [clojure.spec.alpha :as s]

            [cdl.core :as cdl]))

;;; Saves parsed exps into a persistent storage.
;;;

(defprotocol Saver
  (-save [_ exp]))

(deftype NoOpSaver []
  Saver
  (-save [_ exp] exp))

(defn make-no-op-saver [] (->NoOpSaver))

;; Receives parsed exp conforming to the cdl spec and synchronously saves it
;; according with the saver policy.
;; Note: protocol function is wrapped in order to allow spec instrumentation.
(defn save [saver exp]
  (-save saver exp))

(s/fdef save
  :args (s/cat :saver (complement nil?) :input (s/spec ::cdl/exp)))
