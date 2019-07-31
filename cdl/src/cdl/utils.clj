(ns cdl.utils
  (:require [clojure.spec.alpha :as s]
            [clojure.test.check.generators :as gen]
            [clojure.string :as string]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clj-time.coerce :as t-coerce])
  (:import (org.joda.time DateTimeZone DateTime)))

(defn is-str [s] (if (string? s) s nil))

(def gender-decode {"M" "Мужской"
                    "F" "Женский"})

(def race-decode {"1" "Европеоидная"
                  "2" "Монголоидная"
                  "3" "Негроидная"
                  "4" "Другое"})

(def date-time-format
  (tf/formatter (t/time-zone-for-id "UTC")
                "yyyy-MM-dd HH:mm"
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

(def yes-no-decode {"Y" "Да"
                    "N" "Нет"})

(def status-decode {"1" "Разрешилось"
                    "2" "Продолжается"})

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
                          "11" "Состояния наружных половых органов"})
