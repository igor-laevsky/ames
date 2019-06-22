(ns db-api.main
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [com.stuartsierra.component :as component]))

(defn respond-hello [request]
  {:status 200 :body "Hello world!"})

(def routes
  (route/expand-routes
    #{["/greet" :get respond-hello :route-name :greet]}))

(def common-service-map
  {::http/routes            routes
   ::http/resource-path     "/public"
   ::http/type              :jetty
   ::http/port              8080
   ::http/container-options {:h2c? true
                             :h2?  false
                             ;:keystore "test/hp/keystore.jks"
                             ;:key-password "password"
                             ;:ssl-port 8443
                             :ssl? false}})

(defrecord Pedestal [service-map server]
  component/Lifecycle

  (start [this]
    (if server
      this
      (assoc this :server
        (-> service-map
            http/create-server
            http/start))))

  (stop [this]
    (when server
      (http/stop server))
    (assoc this :pedestal-server nil)))

(defn make-pedestal [service-map]
  (map->Pedestal {:service-map service-map}))

(defn -main []
  ;; Start a prod system, join server and stop the system
  (print "Nothing here yet"))
