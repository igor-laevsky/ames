(ns crawler.network
  (:require [clojure.core.async :as a]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [clj-http.client :as http]
            [clj-http.conn-mgr]
            [clj-http.cookies]
            [diehard.core :as dh]
            [diehard.rate-limiter])
  (:refer-clojure :exclude [get])
  (:import (java.util.concurrent Executors ExecutorService)))

;;; Low-level networking functions: manages persistent connections, thread pool,
;;; rate limiting, request failures and so on.

(def common-headers {"User-Agent" "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36"})

(defrecord Network
  [cookie-store conn-mgr ^ExecutorService thread-pool rate-limiter params]

  component/Lifecycle
  (start [{{:keys [num-threads]} :params :as this}]
    (log/info "Starting network service" params)
    (-> this
        (assoc :cookie-store (clj-http.cookies/cookie-store))
        (assoc :conn-mgr (clj-http.conn-mgr/make-reusable-conn-manager
                           {:timeout 10
                            :threads num-threads
                            :default-per-route num-threads
                            :insecure? true}))
        (assoc :thread-pool (Executors/newFixedThreadPool num-threads))
        (assoc :rate-limiter (diehard.rate-limiter/rate-limiter params))))
  (stop [this]
    (log/info "Stopping network service" params)
    (clj-http.conn-mgr/shutdown-manager conn-mgr)
    (.shutdownNow thread-pool)
    this))

;; Creates a non-started network service.
;; 'params' is a map of:
;;   :num-threads - number of threads in a request thread pool
;;   :rate - maximum number of requests per second
(defn make-network [params]
  (map->Network {:params params}))

;; Helper function which performs synchronous request.
;; Never throws an exception. Response is either clj-http response, or a map
;; with a {:status "error"} member and additional information to debug the issue.
(defn- request [network url params]
  (try
    (dh/with-rate-limiter (:rate-limiter network)
      (log/trace "Executing request" {:url url :method (:method params)})
      (http/request (merge
                      {:url                url
                       :cookie-store       (:cookie-store network)
                       :connection-manager (:conn-mgr network)
                       :headers common-headers
                       :socket-timeout     30000
                       :conn-timeout       30000
                       :throw-exceptions   false
                       :redirect-strategy :none}
                      params)))
    (catch Exception e
      {:status "error"
       :msg    (str "Failed to make a network request to the " url)
       :params params
       :cause  e})))

;; Async wrapper over the 'request' function. Delegates request execution to the
;; fixed thread pool. Returns promise chanel.
(defn- async-request [network url params]
  (let [ret-chan (a/promise-chan)
        {^ExecutorService thread-pool :thread-pool} network]

    (log/trace "Scheduling request" {:url url :method (:method params)})
    (.execute thread-pool
              #(a/put! ret-chan
                       (request network url params)))
    ret-chan))

;; Performs an async http get request. 'params' are passed to the clj-http.
;; Returns a promise chanel.
(defn get
  ([network url] (get network url nil))
  ([network url params]
   (async-request network url (merge params {:method :get}))))

;; Same as 'get' but blocks caller thread. Needed to execute request immediately
;; bypassing the thread pool queue.
(defn get!
  ([network url] (get! network url nil))
  ([network url params]
   (request network url (merge params {:method :get}))))

;; Performs an async http post request. 'params' are passed to the clj-http.
;; Returns a promise chanel.
(defn post
  ([network url] (post network url nil))
  ([network url params]
   (async-request network url (merge params {:method :post}))))
