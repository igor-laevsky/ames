(ns cdl.test.vnok
  (:require [clojure.test :refer :all]
            [cdl.vnok :refer :all]))

(deftest foo-bar-test
  (is (= [0 2 4] '(0 2 4))))