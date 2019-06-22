(ns db-api.test.main
  (:require [clojure.test :refer :all]
            [io.pedestal.http :as http]
            [io.pedestal.test :refer [response-for]]
            [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]

            [db-api.main :as main]
            [db-api.pedestal :refer [make-pedestal]]
            [db-api.test.utils :refer [with-system url-for get-service-fn]]))

(def test-service-map
  (-> main/common-service-map
      (merge {:env         :test
              ::http/join? false
              ::http/port  8081})
      (http/default-interceptors)
      (http/dev-interceptors)))

(defn create-test-system []
  (component/system-map
    :pedestal (make-pedestal test-service-map)))

(deftest test-greet
  (with-system [suv (create-test-system)]
    (let [service-fn (get-service-fn suv)
          {:keys [status body]} (response-for service-fn :get (url-for :greet))]
      (is (= 200 status))
      (is (= "Hello world!" body)))))
