(ns dev
  (:require [cdl.common :as c]
            [cdl.vnok :as v]
            [cdl.core :as core]

            [clojure.spec.alpha :as s]
            [clojure.test.check.generators :as gen]
            [clojure.data.json :as js]
            [clojure.java.io :as io]
            [clj-time.core :as t]
            [clj-time.format :as tf]))

;(def t (js/read-str (slurp (io/resource "vnok/DEMO.json")) :key-fn keyword))
;(c/parse-exp-from-json t)
;(core/json->exp t)

;(gen/sample (s/gen ::core/exp))
;(s/exercise (c/get-exp-spec {:type "vnok/DEMO"}))
