(ns db-api.main
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [io.pedestal.http :as http]
            [com.stuartsierra.component :as component]

            [db-api.core :as core]
            [db-api.es :as es]
            [db-api.pedestal :as c]))

(defn -main []
  (let [system (component/start core/prod-system)]
    (.addShutdownHook (Runtime/getRuntime) (Thread. #(do (component/stop system))))
    (.join (get-in system [:pedestal :server ::http/server]))))
