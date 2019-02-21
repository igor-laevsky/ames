(ns cdl.common
  (:require [clojure.spec.alpha :as s]
            [clojure.test.check.generators :as gen]
            [clojure.string :as string]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clj-time.coerce :as t-coerce])
  (:import (org.joda.time DateTimeZone DateTime)))

;;; This namespace contains private interface definitions and helper functions
;;; for the single trial.


;; Common interface for each exp
;;

(defmulti get-exp-spec :type)

(defmulti parse-exp-from-json #(get-in % [:d :FormData :SectionList 0 :ID]))
(defmethod parse-exp-from-json :default [j]
  (throw (ex-info
           (str "Unable to find parser for a given exp.")
           {:orig-id (get-in j [:d :FormData :SectionList 0 :ID])})))


;; Helper routines for specs reused across different experiments
;;

(def non-empty-string? (s/and string? (complement string/blank?)))

(def gender? #{"Женский" "Мужской"})
(def age? (s/int-in 0 200))
(def race? #{"Европеоидная" "Монголоидная" "Негроидная" "Другое"})

(def date-time-format
  (tf/formatter (t/time-zone-for-id "UTC")
                "yyyy-MM-dd hh:mm"
                "yyyy-MM-dd"
                "yyyy-MM"
                "yyyy"))

; Checks that a string is a properly formatted date(time)
(defn date-time-str? [s]
  (try
    (boolean (tf/parse date-time-format s))
    (catch Exception e false)))

(def date-range-gen
  (gen/large-integer*
    {:min (t-coerce/to-long (t/date-time 2018 1 1 00 00 00))
     :max (t-coerce/to-long (t/date-time 2020 1 1 00 00 00))}))

(def date-str-gen
  #(gen/fmap
     (fn [ms] (->> (DateTime. ms DateTimeZone/UTC)
                   (tf/unparse date-time-format)))
     (gen/resize 200 date-range-gen)))

(s/def ::date-time-str (s/with-gen date-time-str? date-str-gen))

;; Common spec for a single experiment
;; {
;;   :type keyword in ES
;;   :visit keyword in ES
;;   :group string
;;   :patient {:name string, :rand-num string, :sex string, :birthday date]
;;   :location string
;;   :finished boolean
;;   :verified boolean
;; }
;;

(s/def ::type non-empty-string?)
(s/def ::visit non-empty-string?)
(s/def ::group string?)

(s/def ::name non-empty-string?)
(s/def ::rand-num non-empty-string?)
(s/def ::birthday ::date-time-str)
(s/def ::sex #{"M" "F"})
(s/def ::patient (s/keys :req-un [::name]
                         :opt-un [::rand-num ::sex ::birthday]))
(s/def ::location non-empty-string?)
(s/def ::finished boolean?)
(s/def ::verified boolean?)

(s/def ::exp-common
  (s/keys :req-un [::type ::visit ::group ::patient
                   ::location ::finished ::verified]))
