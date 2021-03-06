(ns crawler.test.etl
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.core.async :as a]
            [clojure.data.json :as js]
            [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]

            [crawler.etl :as etl]
            [crawler.network :as network]
            [crawler.saver :as saver]
            [cdl.core :as cdl]))

(def creds (read-string (slurp (io/reader (io/resource "creds.edn")))))

(def saver-file "test/resources/saver/tmp.json")

(defn create-test-system [should-login?]
  (-> (component/system-map
        :network (network/make-network {:num-threads 10
                                        :rate        2})
        :saver (saver/make-file-saver {:file-name saver-file})
        :etl (component/using
               (if should-login?
                 (etl/make-login-etl creds)
                 (etl/make-plain-etl creds))
               [:network :saver]))))

(defn create-and-start
  ([] (create-and-start true))
  ([should-login?]
   (-> (create-test-system should-login?) (component/start))))

(deftest test-login-logout
  (let [{:keys [etl] :as system} (create-and-start false)]
    (try
      ; Can't get system map before we login
      (is (= 302 (-> (etl/subject-matrix etl) (a/<!!) (:status))))
      ; Should be able to login
      (is (= true (etl/login! etl)))
      ; Login is idempotent
      (is (= true (etl/login! etl)))
      ; Now we can get subject matrix
      (is (= 200 (-> (etl/subject-matrix etl) (a/<!!) (:status))))
      ; Should be able to logout
      (is (= 200 (-> (etl/logout! etl) (:status))))
      ; No subject matrix now
      (is (= 302 (-> (etl/subject-matrix etl) (a/<!!) (:status))))
      (finally (component/stop system)))))

(deftest auto-login-logout
  (let [{:keys [etl] :as system} (create-and-start)]
    (try
      (is (= 200 (-> (etl/subject-matrix etl) (a/<!!) (:status))))
      (finally (component/stop system)))))

(deftest test-get-visits
  (let [{:keys [etl] :as system} (create-and-start)]
    (try
      (is (= #{"se.V9&rk=0"
               "se.V10&rk=0"
               "se.UV&rk=2"
               "se.AE&rk=0"
               "se.DV&rk=0"
               "se.V1&rk=0"
               "se.EOS&rk=0"
               "se.V3&rk=0"
               "se.UV&rk=1"
               "se.V5&rk=0"
               "se.V7&rk=0"
               "se.UV&rk=0"
               "se.V2&rk=0"
               "se.V11&rk=0"
               "se.SCR1&rk=0"
               "se.V8&rk=0"
               "se.BB&rk=0"
               "se.V6&rk=0"
               "se.LB.OTH&rk=0"
               "se.V4&rk=0"
               "se.CM&rk=0"
               "se.CO&rk=0"
               "se.SCR&rk=0"
               "se.PR&rk=0"
               "se.V12&rk=0"}
             (etl/get-visits! etl)))
      (finally (component/stop system)))))

(deftest test-visit-exp-service
  (let [{:keys [etl] :as system} (create-and-start)]
    (try
      (let [from-chan (a/to-chan ["se.V9&rk=0"])
            to-chan (a/chan 100)
            serv (etl/start-visit-exp-service etl from-chan to-chan)
            res (a/<!! (a/into [] to-chan))]
        (is (= [{:id "228", :context {:rand-num "R001"}}
                {:id "229", :context {:rand-num "R001"}}
                {:id "423", :context {:rand-num "R001"}}
                {:id "424", :context {:rand-num "R001"}}
                {:id "425", :context {:rand-num "R001"}}
                {:id "426", :context {:rand-num "R001"}}
                {:id "1346", :context {:rand-num "R368"}}]
               (take 7 res)))
        ; Service should close to-chan once from-chan is closed
        (is (= false (a/put! to-chan :nothing)))
        ; Check that service had stopped
        (is (= false (a/put! serv :nothing))))
      (finally (component/stop system)))))

(deftest test-parse-exp-service
  (let [{:keys [etl] :as system} (create-and-start)]
    (try
      (let [from-chan (a/to-chan [{:id "88", :context {:rand-num "R462"}}])
            to-chan (a/chan 100)
            serv (etl/start-parse-exp-service (:etl system) from-chan to-chan)
            res (a/<!! to-chan)]
        (is (= {:name "01-002",
                :birthday "1939-10-06",
                :gender "Мужской",
                :rand-num "R462"}
               (:patient res)))
        (is (= "2018-05-28" (:date res)))
        (is (= "2018-05-28 07:39" (:agr-datetime res)))
        (is (= "vnok/DEMO") (:type res))
        ; to-chan is closed and service is stopped
        (is (= false (a/put! to-chan :nothing)))
        (is (= false (a/put! serv :nothing))))
      (finally (component/stop system)))))

(deftest test-save-exp-service
  (let [{:keys [etl] :as system} (create-and-start)]
    (try
      (let [exps (a/to-chan [{:id "88", :context {:rand-num "R462"}}])
            parsed-exps (a/chan 100)
            p-e (etl/start-parse-exp-service (:etl system) exps parsed-exps)
            sv (etl/start-save-exp-service (:etl system) parsed-exps)]
        ; Wait for saver to finish saving
        (a/<!! sv)
        ; Re-load saved json
        (let [saved-exp (js/read (io/reader "test/resources/saver/tmp.json")
                                 :key-fn keyword)]
          (is (s/valid? ::cdl/exp saved-exp))
          (is (= {:name "01-002",
                  :birthday "1939-10-06",
                  :gender "Мужской",
                  :rand-num "R462"}
                 (:patient saved-exp)))))
      (finally (component/stop system)))))
