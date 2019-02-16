(ns dev
  (:require [cdl.common :as c]
            [cdl.vnok :as v]
            [cdl.core :as core]

            [clojure.spec.alpha :as s]
            [clojure.test.check.generators :as gen]
            [clojure.data.json :as js]
            [clojure.java.io :as io]))

;(s/exercise ::c/exp-common)
;(s/exercise ::v/vnok-common)
;
;(c/parse-exp-from-json {})

;(def t (js/read-str (slurp (io/resource "vnok/DEMO.json")) :key-fn keyword))
;(c/parse-exp-from-json t)
;(core/json->exp t)
;(s/get-spec ::core/exp)
;(gen/sample (s/gen ::core/exp))
;
;(s/exercise (c/get-exp-spec {:type "vnok/DEMO"}))
;
;(s/exercise (c/get-exp-spec {:type "vnok/DEMO"}))