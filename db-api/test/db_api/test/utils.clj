(ns db-api.test.utils
  (:require [clojure.test :refer :all]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [com.stuartsierra.component :as component]

            [db-api.main :as main]))

(defmacro with-system
  [[bound-var binding-expr] & body]
  `(let [~bound-var (component/start ~binding-expr)]
     (try
       ~@body
       (finally
         (component/stop ~bound-var)))))

(def url-for (route/url-for-routes main/routes))

(defn get-service-fn [system]
  (get-in system [:pedestal :server ::http/service-fn]))
