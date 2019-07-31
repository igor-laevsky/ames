(ns cdl.core
  (:require [cdl.common :as common]
            [cdl.vnok :as vnok]
            [clojure.spec.alpha :as s]))

;;; This namespace defines public interface for the set of trials.

; Main spec to be used to check specific experiment.
(s/def ::exp (s/get-spec ::vnok/exp))

; Return true if we can parse given exp
(defn can-parse? [json]
  (contains?
    (methods vnok/parse-exp-from-json)
    (common/dispatch-parser json)))

; Takes a json object and returns experiment or raises an exception.
(defn json->exp [json]
  (vnok/parse-exp-from-json json))

(s/fdef json->exp
  :args (s/cat :json map?)
  :ret ::exp)
