(ns cdl.common
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as g]
            [clojure.string :as string]))

;;; This namespace contains private interface definitions and helper functions
;;; for the single trial.

;; Common interface for each exp
(defmulti get-exp-spec :type)
(defmethod get-exp-spec :default [_]
  (throw (ex-info "Unable to find spec for a given exp." {})))
  ;(s/spec string?))

(defmulti parse-exp-from-json #(get-in % [:d :FormData :SectionList 0 :ID]))
(defmethod parse-exp-from-json :default [j]
  (throw (ex-info
           (str "Unable to find parser for a given exp.")
           {:orig-id (get-in j [:d :FormData :SectionList 0 :ID])})))



;; Common spec for the single experiment
;; {
;;   :type keyword in ES
;;   :visit keyword in ES
;;   :group string
;;   :patient {:name string, :rand-num string, :sex string, :birthday date]
;;   :location string
;;   :finished boolean
;;   :verified boolean
;; }
(def ^:private non-empty-string? (s/and string? (complement string/blank?)))

(s/def ::type non-empty-string?)
(s/def ::visit non-empty-string?)
(s/def ::group string?)

(s/def ::name non-empty-string?)
(s/def ::rand-num non-empty-string?)
(s/def ::sex #{"M" "F"})
(s/def ::birthday non-empty-string?)
(s/def ::patient (s/keys :req-un [::name]
                         :opt-un [::rand-num ::sex ::birthday]))
(s/def ::location non-empty-string?)
(s/def ::finished boolean?)
(s/def ::verified boolean?)

(s/def ::exp-common
  (s/keys :req-un [::type ::visit ::group ::patient
                   ::location ::finished ::verified]))
