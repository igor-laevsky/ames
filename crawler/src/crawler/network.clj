(ns crawler.network
  (:require [clojure.core.async :as a]
            [com.stuartsierra.component :as component]
            [clj-http.client :as http]
            [clj-http.conn-mgr]
            [clj-http.cookies])
  (:refer-clojure :exclude [get]))

;;; Low-level networking functions: manages persistent connections, thread pool,
;;; request frequency, request failures and so on.

(defrecord Network [num-threads cookie-store conn-mgr thread-pool]
  component/Lifecycle

  (start [this]
    (println "Starting network service")
    (-> this
        (assoc :cookie-store (clj-http.cookies/cookie-store))
        (assoc :conn-mgr (clj-http.conn-mgr/make-reusable-conn-manager
                           {:timeout 10
                            :threads num-threads
                            :default-per-route num-threads
                            :insecure? true}))))
  (stop [this]
    (println "Stopping network service")
    (clj-http.conn-mgr/shutdown-manager conn-mgr)
    this))

(defn make-network [num-threads]
  (map->Network {:num-threads num-threads}))

(defn- request [network req-func url params]
  (let [c (a/promise-chan)]
    (a/put!
      c
      (req-func url (merge
                      {:cookie-store       (:cookie-store network)
                       :connection-manager (:conn-mgr network)}
                      params)))
    c))

(defn get
  ([network url] (get network url nil))
  ([network url params] (request network http/get url params)))

(defn post
  ([network url] (post network url nil))
  ([network url params] (request network http/post url params)))
