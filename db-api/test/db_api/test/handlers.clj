(ns db-api.test.handlers
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [io.pedestal.http :as http]
            [io.pedestal.test :refer [response-for]]
            [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]

            [db-api.core :as main]
            [db-api.pedestal :refer [make-pedestal]]
            [db-api.es :as es]
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
    :es (es/make-es {:host "http://127.0.0.1:9200" :index "vnok_test"})
    :pedestal (component/using
                (make-pedestal test-service-map)
                [:es])))

(deftest test-search
  (with-system [sys (create-test-system)]
    (let [service-fn (get-service-fn sys)
          make-request (fn [params] (response-for service-fn
                                                  :get
                                                  (url-for :search
                                                           :query-params params)))]
      (let [{:keys [status body]} (make-request {:q "01-002"})
            parsed-body (json/read-str body :key-fn keyword)]
        (is (= 200 status))
        (is (= 11 (:total parsed-body)))
        (is (= 11 (count (:hits parsed-body)))))
      (let [{:keys [status body]} (make-request {:q "01-002" :size "2"})
            parsed-body (json/read-str body :key-fn keyword)]
        (is (= 200 status))
        (is (= 11 (:total parsed-body)))
        (is (= 2 (count (:hits parsed-body)))))
      (let [{:keys [status body]} (make-request {:q "01-002" :size 2 :from 10})
            parsed-body (json/read-str body :key-fn keyword)]
        (is (= 200 status))
        (is (= 11 (:total parsed-body)))
        (is (= 1 (count (:hits parsed-body)))))
      (let [{:keys [status body]} (make-request
                                    {:q "location:01 AND
                                         patient:01-003 AND
                                         visit:vnok/se.SCR"})
            parsed-body (json/read-str body :key-fn keyword)]
        (is (= 200 status))
        (is (not= 0 (:total parsed-body)))
        (is (not= 0 (count (:hits parsed-body))))))))

(deftest test-locations
  (with-system [sys (create-test-system)]
    (let [{:keys [status body]} (response-for (get-service-fn sys)
                                              :get
                                              (url-for :locations))
          parsed-body (json/read-str body :key-fn keyword)]
      (is (= 200 status))
      (is (= "03" (:name (first (take-last 2 parsed-body)))))
      (is (= 316 (-> parsed-body first :total)))
      (is (= 308 (-> parsed-body first :verified)))
      (is (= 134 (-> parsed-body second :verified))))))

(deftest test-patients
  (with-system [sys (create-test-system)]
    (let [{:keys [status body]} (response-for (get-service-fn sys)
                                              :get
                                              (url-for :patients
                                                       :query-params {:loc "03"}))
          parsed-body (json/read-str body :key-fn keyword)]
      (is (= 200 status))
      (is (= "03-001" (-> parsed-body first :name)))
      (is (= "R008" (-> parsed-body first :rand-num)))
      (is (= "not found" (-> parsed-body last (get :rand-num "not found"))))
      (is (= 11 (-> parsed-body second :total)))
      (is (= 0 (-> parsed-body second :verified))))))

(deftest test-visits
  (with-system [sys (create-test-system)]
    (let [{:keys [status body]} (response-for (get-service-fn sys)
                                              :get
                                              (url-for :visits
                                                       :query-params {:loc "01"
                                                                      :pat "01-003"}))
          parsed-body (json/read-str body :key-fn keyword)]
      (is (= 200 status))
      (is (not= 0 (count parsed-body))))))

(deftest test-exps
  (with-system [sys (create-test-system)]
    (let [{:keys [status body]} (response-for (get-service-fn sys)
                                              :get
                                              (url-for :exps
                                                       :query-params {:name "01-003"}))
          parsed-body (json/read-str body :key-fn keyword)]
      (is (= 200 status))
      (is (every? #(= (get-in % [:patient :name]) "01-003") parsed-body))
      (is (every? #(= (:location %) "01") parsed-body))
      (is (= "vnok/PE.v2-v10" (-> parsed-body first :type)))
      (is (= "vnok/DEMO" (-> parsed-body last :type))))))
