(ns cdl.test.vnok
  (:require [clojure.test :refer :all]
            [cdl.core :as c]
            [clojure.data.json :as js]
            [clojure.java.io :as io]))

(defn read-exp-from-file [fname]
  (-> fname
      (io/resource)
      (slurp)
      (js/read-str :key-fn keyword)
      (c/json->exp)))

(deftest vnok.DEMO-test
  (is
    (= {:patient      {:name     "01-002",
                       :birthday "1939-10-06",
                       :gender   "Мужской",
                       :rand-num ""},
        :date         "2018-05-28",
        :group        "",
        :age          78,
        :race         "Европеоидная",
        :other        "",
        :birthday     "1939-10-06",
        :type         "vnok/DEMO",
        :finished     true,
        :visit        "vnok/se.SCR",
        :agr-datetime "2018-05-28 07:39",
        :gender       "Мужской",
        :location     "01",
        :verified     true}
    (read-exp-from-file "vnok/DEMO.json"))))
