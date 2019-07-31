(ns dev
  (:require [cdl.utils :as c]
            [cdl.vnok :as v]
            [cdl.core :as core]

            [clojure.spec.alpha :as s]
            [clojure.test.check.generators :as gen]
            [clojure.data.json :as js]
            [clojure.java.io :as io]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [json-path :as jp]))

;(def t (js/read-str (slurp (io/resource "vnok/UV.json")) :key-fn keyword))
;(def tt (c/parse-exp-from-json t))
;(core/json->exp t)
;(s/explain ::core/exp tt)
;(s/valid? ::core/exp tt)
;
;(def t (c/get-exp-spec {:type "vnok/PE.v2-v10"}))
;(gen/sample (s/gen t))
;
;(gen/sample (s/gen ::core/exp))
;(s/exercise (c/get-exp-spec {:type "vnok/DEMO"}))
(defn read-json-from-file [fname]
  (-> fname
      (io/resource)
      (slurp)
      (js/read-str :key-fn keyword)))

(def j (read-json-from-file "vnok/DEMO.json"))
(jp/at-path "$.context.rand-num" j)
