(ns cdl.test.vnok
  (:require [clojure.test :refer :all]
            [cdl.core :as c]
            [clojure.data.json :as js]
            [clojure.java.io :as io]
            [orchestra.spec.test :as st]))

(st/instrument)

(defn read-exp-from-file [fname]
  (-> fname
      (io/resource)
      (slurp)
      (js/read-str :key-fn keyword)
      (c/json->exp)))

(deftest vnok.DEMO-test
  (is
    (= {:patient      {:name     "01-002",
                       :birthday "1939-10-06",
                       :gender   "Мужской",
                       :rand-num ""},
        :date         "2018-05-28",
        :group        "",
        :age          78,
        :race         "Европеоидная",
        :other        "",
        :birthday     "1939-10-06",
        :type         "vnok/DEMO",
        :finished     true,
        :visit        "vnok/se.SCR",
        :agr-datetime "2018-05-28 07:39",
        :gender       "Мужской",
        :location     "01",
        :verified     true}
    (read-exp-from-file "vnok/DEMO.json"))))

(deftest vnok.MD-test
  (is
    (= {:patient {:name "01-002",
           :birthday "1939-10-06",
           :gender "Мужской",
           :rand-num ""},
 :has-records "Да",
 :group "",
 :type "vnok/MD",
 :finished true,
 :records [{:status "Продолжается",
            :condition "Артериальная гипертензия II стадии, риск 3, контролируемая. ",
            :stop-date "",
            :start-date "2013"}
           {:status "Продолжается",
            :condition "Хроническая сердечная недостаточность I стадии, ФК 2",
            :stop-date "",
            :start-date "2013"}
           {:status "Разрешилось",
            :condition "Экстрасиcтолия желудочковая, редкая одиночная",
            :stop-date "2018-05-28",
            :start-date "2016-08-31"}
           {:status "Продолжается",
            :condition "Дислипидемия",
            :stop-date "",
            :start-date "2016-09-03"}
           {:status "Продолжается",
            :condition "Сахарный диабет 2 типа, ИЦУГ < 7,5%",
            :stop-date "",
            :start-date "2015"}
           {:status "Продолжается",
            :condition "Атеросклероз магистральных артерий головы (клинически)",
            :stop-date "",
            :start-date "2018-05-28"}
           {:status "Продолжается",
            :condition "Синдром позвоночно-подключичного обкрадывания слева (клинически)",
            :stop-date "",
            :start-date "2018-05-28"}
           {:status "Разрешилось",
            :condition "Миграция водителя ритма по предсердиям",
            :stop-date "2018-05-28",
            :start-date "2018-05-28"}
           {:status "Разрешилось",
            :condition "Замедление атриовентрикулярного проведения",
            :stop-date "2018-05-28",
            :start-date "2018-05-28"}
           {:status "Разрешилось",
            :condition "Перелом правой верхней конечности",
            :stop-date "1980",
            :start-date "1980"}
           {:status "Разрешилось",
            :condition "Аппендэктомия ",
            :stop-date "1966",
            :start-date "1966"}
           {:status "Разрешилось",
            :condition "герниопластика паховой грыжи слева ",
            :stop-date "2009",
            :start-date "2009"}
           {:status "Продолжается",
            :condition "Боль в левой ноге, вынуждающая остановиться при ходьбе около 100 метров",
            :stop-date "",
            :start-date "2017-10"}],
        :visit "vnok/se.SCR",
        :location "01",
        :verified true}
    (read-exp-from-file "vnok/MD.json"))))
