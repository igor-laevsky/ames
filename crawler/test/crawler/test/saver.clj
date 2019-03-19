(ns crawler.test.saver
  (:require [clojure.test :refer :all]
            [clojure.data.json :as js]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [orchestra.spec.test :as st]
            [com.stuartsierra.component :as component]

            [crawler.saver :as saver]
            [cdl.core :as cdl]))

(defn read-exp-from-file [fname]
  (-> fname
      (io/resource)
      (slurp)
      (js/read-str :key-fn keyword)
      (cdl/json->exp)))

(deftest test-file-saver
  (let [s (component/start (saver/make-file-saver
                             {:file-name "test/resources/saver/tmp.json"}))
        exp (read-exp-from-file "vnok/DEMO.json")]
    (saver/save s exp)
    (component/stop s)
    (let [loaded-exp (js/read (io/reader "test/resources/saver/tmp.json")
                              :key-fn keyword)]
      (is (s/valid? ::cdl/exp loaded-exp))
      (is (= exp loaded-exp)))
    (io/delete-file "test/resources/saver/tmp.json")))
