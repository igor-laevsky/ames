(ns crawler.network
  (:require [com.stuartsierra.component :as component]))

;;; Low-level networking functions, manages persistent connections, thread pool,
;;; request frequency, request failures and so on.

(defrecord Network [num-threads]
  component/Lifecycle

  (start [this]
    (println "Starting network service")
    this)
  (stop [this]
    (println "Stoping network service")
    this))

(defn make-network [num-threads]
  (map->Network {:num-threads num-threads}))

