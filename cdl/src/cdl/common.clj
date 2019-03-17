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

(defn dispatch-parser [json] (get-in json [:d :FormData :SectionList 0 :ID]))
(defmulti parse-exp-from-json dispatch-parser)
(defmethod parse-exp-from-json :default [j]
  (throw (ex-info
           (str "Unable to find parser for a given exp.")
           {:orig-id (get-in j [:d :FormData :SectionList 0 :ID])})))


;; Helper routines for specs reused across different experiments
;;

(s/def ::non-empty-string (s/and string? (complement string/blank?)))

(def gender-decode {"M" "Мужской"
                    "F" "Женский"
                    "" ""})
(s/def ::gender (set (vals gender-decode)))

(def race-decode {"1" "Европеоидная"
                  "2" "Монголоидная"
                  "3" "Негроидная"
                  "4" "Другое"
                  "" ""})
(s/def ::race (set (vals race-decode)))

; TODO: Can do better than this
(s/def ::age string?)

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
(def empty-date-str-gen
  #(gen/one-of [(date-str-gen) (gen/return "")]))

(s/def ::date-time-str (s/with-gen date-time-str? date-str-gen))
(s/def ::date-time-str-or-nil
  (s/with-gen
    (s/and string? (s/or :date ::date-time-str :empty empty?))
    empty-date-str-gen))

(def yes-no-decode {"Y" "Да"
                    "N" "Нет"
                    "" ""})
(s/def ::yes-no (set (vals yes-no-decode)))

(def status-decode {"1" "Разрешилось"
                    "2" "Продолжается"
                    "" ""})
(s/def ::status (set (vals status-decode)))

(def organ-system-decode {"1" "Общее состояние"
                          "2" "Кожные покровы и видимые слизистые"
                          "3" "Состояния органов головы и шеи"
                          "4" "Периферическая и центральная нервная система"
                          "5" "Костно-мышечный аппарат"
                          "6" "Периферические лимфатические узлы"
                          "7" "Дыхательная система"
                          "8" "Сердечно-сосудистая система"
                          "9" "Пищеварительная система"
                          "10" "Мочевыделительная система"
                          "11" "Состояния наружных половых органов"
                          "" ""})
(s/def ::organ-system (set (vals organ-system-decode)))


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

(s/def ::type ::non-empty-string)
(s/def ::visit ::non-empty-string)
(s/def ::group string?)

(s/def ::name ::non-empty-string)
(s/def ::rand-num string?)
(s/def ::birthday ::date-time-str)
(s/def ::patient (s/keys :req-un [::name]
                         :opt-un [::rand-num ::gender ::birthday]))
(s/def ::location ::non-empty-string)
(s/def ::finished boolean?)
(s/def ::verified boolean?)

(s/def ::exp-common
  (s/keys :req-un [::type ::visit ::group ::patient
                   ::location ::finished ::verified]))
