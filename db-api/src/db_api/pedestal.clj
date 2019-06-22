(ns db-api.pedestal
  (:require [io.pedestal.http :as http]
            [io.pedestal.interceptor :refer [interceptor]]
            [com.stuartsierra.component :as component]))

(defn make-pedestal-inserter [p]
  (interceptor
    {:name  :pedestal-inserter
     :enter (fn [ctx] (assoc ctx ::pedestal p))}))

(defrecord Pedestal [service-map server]
  component/Lifecycle

  (start [this]
    (if server
      this
      (assoc this :server
        (-> service-map
            (update ::http/interceptors conj (make-pedestal-inserter this))
            http/create-server
            http/start))))

  (stop [this]
    (when server
      (http/stop server))
    (assoc this :pedestal-server nil)))

(defn make-pedestal [service-map]
  (map->Pedestal {:service-map service-map}))

(defn get-component [ctx key]
  (if-let [component (get-in ctx [::pedestal key] ::not-found)]
    component
    (throw (ex-info (str "Unable to attach component " key) {}))))

(defn using-component [key]
  (interceptor {:name  :using-component
                :enter (fn [ctx]
                         (assoc-in ctx [:request ::component key]
                                   (get-component ctx key)))}))

(defn use-component [req key] (get-in req [::component key]))
