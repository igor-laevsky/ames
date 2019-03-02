(ns dev
  (:require [cdl.common :as c]
            [cdl.vnok :as v]
            [cdl.core :as core]

            [clojure.spec.alpha :as s]
            [clojure.test.check.generators :as gen]
            [clojure.data.json :as js]
            [clojure.java.io :as io]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [json-path :as jp]))

(def t (js/read-str (slurp (io/resource "vnok/PE.v2-v10.json")) :key-fn keyword))
(def tt (c/parse-exp-from-json t))
(core/json->exp t)
(s/explain ::core/exp tt)
(s/valid? ::core/exp tt)

(def t (c/get-exp-spec {:type "vnok/PE.v2-v10"}))
(gen/sample (s/gen t))

(gen/sample (s/gen ::core/exp))
;(s/exercise (c/get-exp-spec {:type "vnok/DEMO"}))
