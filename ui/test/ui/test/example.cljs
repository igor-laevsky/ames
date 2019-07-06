(ns ui.test.example
  (:require [cljs.test :refer-macros [deftest is testing]]))

(deftest multiply-test
  (is (= 2 (* 1 2))))

(deftest multiply-test-2
  (is (= (* 75 10) (* 10 75))))
