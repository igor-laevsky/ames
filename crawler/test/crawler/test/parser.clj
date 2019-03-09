(ns crawler.test.parser
  (:require [clojure.test :refer :all]
            [crawler.parser :as p]
            [clojure.java.io :as io]))

(defn get-test-file [fname]
  (-> (str "parser/" fname)
      (io/resource)
      (slurp)))

(deftest parseextract-viewstate
  (is (= {"__EVENTTARGET" ""
          "__EVENTARGUMENT" ""
          "__VIEWSTATE" "/wEPDwUKMTc4MjUyNjY2MmRkWxtguUnhMfRdoRX8nOa6pvO6yqW0KVtFGgQgvJ2fmT4="
          "__VIEWSTATEGENERATOR" "F098D98A"}
         (p/extract-viewstate (get-test-file "subject-matrix-c01.html"))
         )))
