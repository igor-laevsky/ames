(ns crawler.test.parser
  (:require [clojure.test :refer :all]
            [crawler.parser :as p]
            [clojure.java.io :as io]))

(defn get-test-file [fname]
  (-> (str "parser/" fname)
      (io/resource)
      (slurp)))

(deftest test-extract-viewstate
  (is (= {"__EVENTTARGET" ""
          "__EVENTARGUMENT" ""
          "__VIEWSTATE" "/wEPDwUKMTc4MjUyNjY2MmRkWxtguUnhMfRdoRX8nOa6pvO6yqW0KVtFGgQgvJ2fmT4="
          "__VIEWSTATEGENERATOR" "F098D98A"}
         (p/extract-viewstate (get-test-file "subject-matrix-c01.html"))
         )))

(deftest test-extract-visits
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
         (p/extract-visits (get-test-file "subject-matrix-c01.html")))))