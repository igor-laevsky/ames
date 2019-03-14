(ns crawler.test.etl
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.core.async :as a]
            [com.stuartsierra.component :as component]

            [crawler.etl :as etl]
            [crawler.network :as network]
            [crawler.saver :as saver]))

(def creds (read-string (slurp (io/reader (io/resource "creds.edn")))))

(defn create-test-system [should-login?]
  (-> (component/system-map
        :network (network/make-network {:num-threads 10
                                        :rate        10})
        :saver (saver/make-file-saver {:file-name "test/resources/saver/tmp.json"})
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
