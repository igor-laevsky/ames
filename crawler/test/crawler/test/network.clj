(ns crawler.test.network
  (:require [clojure.test :refer :all]
            [clojure.core.async :as a]
            [com.stuartsierra.component :as component]
            [clj-http.fake :as fake]
            [clj-http.cookies :as cookies]
            [clojure.pprint :refer [pprint]]
            [crawler.network :as network]))

(def test-system nil)

(defn create-test-system []
  (component/system-map
    :network (network/make-network 10)))

(def fake-routes
  {"https://google.com"
   {:get (fn [r] {:status 200})}

   "https://test.test"
   {:get (fn [r] {:status 400})
    :post (fn [r] {:status 200})}
   })

(defn start-system []
  (alter-var-root #'test-system (constantly (create-test-system)))
  (alter-var-root #'test-system component/start))

(defn stop-system []
  (alter-var-root #'test-system component/stop)
  (alter-var-root #'test-system (constantly nil)))

(defn system-fixture [f]
  (start-system)
  (f)
  (stop-system))

(use-fixtures :once system-fixture)

(deftest get-test
  (fake/with-global-fake-routes-in-isolation
    fake-routes
    (is (=
          (:status (a/<!! (network/get (:network test-system) "https://google.com")))
          200))))

(deftest post-test
  (fake/with-global-fake-routes-in-isolation
    fake-routes
    (is (=
          (:status (a/<!! (network/post (:network test-system) "https://test.test")))
          200))))

; Check that we correctly handle non-200 http responses
(deftest http-error-test
  (fake/with-global-fake-routes-in-isolation
    fake-routes
    (is (=
          (:status (a/<!! (network/get (:network test-system) "https://test.test")))
          400))))

; TODO: Mock the request
(deftest cookie-test
  (is (= (do
           (a/<!! (network/get (:network test-system) "https://google.com"))
           (keys (cookies/get-cookies (get-in test-system [:network :cookie-store]))))
         ["1P_JAR" "NID"])))
