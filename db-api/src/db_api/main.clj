(ns db-api.main
  (:require [clojure.tools.logging :as log]
            [io.pedestal.http :as http]
            [com.stuartsierra.component :as component]

            [db-api.handlers :as handlers]
            [db-api.es :as es]
            [db-api.pedestal :as c]))

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

(def prod-service-map
  (merge common-service-map {:env         :prod
                             ::http/join? false}))

(def prod-system
  (component/system-map
    :es (es/make-es {:host "http://localhost:9200" :index "vnok"})
    :pedestal (component/using
                (c/make-pedestal prod-service-map)
                [:es])))

(defn -main []
  (log/trace (::http/join prod-system))
  (let [system (component/start prod-system)]
    (.addShutdownHook (Runtime/getRuntime) (Thread. #(do
                                                       (log/trace "Shutting down")
                                                       (component/stop system))))
    (.join (get-in system [:pedestal :server ::http/server]))))
