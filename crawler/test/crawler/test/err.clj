(ns crawler.test.err
  (:require [crawler.err :as e]
            [clojure.test :refer :all]))

(def always-fail (constantly (e/rr "Not good!")))
(defn sometimes-fail [x]
  (case x
    :good 1
    :normal nil
    :bad (e/rr "This is bad!")))
(defn always-good [x]
  (inc x))

(deftest test-check-err
  (is (e/rr? (e/rr "Hello")))
  (is (not (e/rr? "Not err"))))

(deftest test-deref
  (let [err (e/rr "Hello")]
    (is (= @err "Hello"))))

(deftest test-thread-first
  (is (= (e/rr-> :good
                 (sometimes-fail)
                 (always-good))
         2))
  (let [err (e/rr-> :bad
                 (sometimes-fail)
                 (always-good))]
    (is (e/rr? err))
    (is (= @err "This is bad!"))))
