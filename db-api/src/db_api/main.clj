(ns db-api.main
  (:require [io.pedestal.http :as http]
            [com.stuartsierra.component :as component]

            [db-api.handlers :as handlers]))

(def common-service-map
  {::http/routes            handlers/routes
   ::http/resource-path     "/public"
   ::http/type              :jetty
   ::http/port              8080
   ::http/container-options {:h2c? true
                             :h2?  false
                             ;:keystore "test/hp/keystore.jks"
                             ;:key-password "password"
                             ;:ssl-port 8443
                             :ssl? false}})

(defn -main []
  ;; Start a prod system, join server and stop the system
  (print "Nothing here yet"))
