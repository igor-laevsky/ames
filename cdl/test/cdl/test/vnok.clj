(ns cdl.test.vnok
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.data.json :as js]
            [clojure.java.io :as io]

            [cdl.core :as c]))

(defn read-json-from-file [fname]
  (-> fname
      (io/resource)
      (slurp)
      (js/read-str :key-fn keyword)))

(defn read-exp-from-file [fname]
  (-> (read-json-from-file fname)
      (c/json->exp)))

(deftest test-can-parse
  (is (c/can-parse? (read-json-from-file "vnok/DEMO.json")))
  (is (c/can-parse? (read-json-from-file "vnok/MD.json")))
  (is (not (c/can-parse? {}))))

(deftest vnok.DEMO-test
  (is
    (= {:patient {:name "01-002",
                  :birthday "1939-10-06",
                  :gender "Мужской",
                  :rand-num "R462"},
        :date "2018-05-28",
        :age "78",
        :race "Европеоидная",
        :birthday "1939-10-06",
        :type "vnok.DEMO",
        :finished true,
        :visit "vnok/se.SCR",
        :agr-datetime "2018-05-28 07:39",
        :gender "Мужской",
        :location "01",
        :verified true}
       (read-exp-from-file "vnok/DEMO.json"))))

(deftest vnok.MD-test
  (is
    (= {:patient     {:name     "01-002",
                      :birthday "1939-10-06",
                      :gender   "Мужской"},
        :has-records "Да",
        :type        "vnok.MD",
        :finished    true,
        :records     [{:status     "Продолжается",
                       :condition  "Артериальная гипертензия II стадии, риск 3, контролируемая.",
                       :start-date "2013"}
                      {:status     "Продолжается",
                       :condition  "Хроническая сердечная недостаточность I стадии, ФК 2",
                       :start-date "2013"}
                      {:status     "Разрешилось",
                       :condition  "Экстрасиcтолия желудочковая, редкая одиночная",
                       :stop-date  "2018-05-28",
                       :start-date "2016-08-31"}
                      {:status     "Продолжается",
                       :condition  "Дислипидемия",
                       :start-date "2016-09-03"}
                      {:status     "Продолжается",
                       :condition  "Сахарный диабет 2 типа, ИЦУГ < 7,5%",
                       :start-date "2015"}
                      {:status     "Продолжается",
                       :condition  "Атеросклероз магистральных артерий головы (клинически)",
                       :start-date "2018-05-28"}
                      {:status     "Продолжается",
                       :condition  "Синдром позвоночно-подключичного обкрадывания слева (клинически)",
                       :start-date "2018-05-28"}
                      {:status     "Разрешилось",
                       :condition  "Миграция водителя ритма по предсердиям",
                       :stop-date  "2018-05-28",
                       :start-date "2018-05-28"}
                      {:status     "Разрешилось",
                       :condition  "Замедление атриовентрикулярного проведения",
                       :stop-date  "2018-05-28",
                       :start-date "2018-05-28"}
                      {:status     "Разрешилось",
                       :condition  "Перелом правой верхней конечности",
                       :stop-date  "1980",
                       :start-date "1980"}
                      {:status     "Разрешилось",
                       :condition  "Аппендэктомия",
                       :stop-date  "1966",
                       :start-date "1966"}
                      {:status     "Разрешилось",
                       :condition  "герниопластика паховой грыжи слева",
                       :stop-date  "2009",
                       :start-date "2009"}
                      {:status     "Продолжается",
                       :condition  "Боль в левой ноге, вынуждающая остановиться при ходьбе около 100 метров",
                       :start-date "2017-10"}],
        :visit       "vnok/se.SCR",
        :location    "01",
        :verified    true}
       (read-exp-from-file "vnok/MD.json"))))

(deftest vnok.PE.v2-v10-test
  (is
    (=
      {:patient          {:name     "12-004",
                          :birthday "1951-11-05",
                          :gender   "Мужской"},
       :date             "2018-09-15",
       :type             "vnok.PE.v2-v10",
       :examination-date "2018-09-15 08:50",
       :is-done          true,
       :finished         true,
       :visit            "vnok/se.V2",
       :deviations       [{:is-important "Нет",
                           :organ-system "Дыхательная система",
                           :comment      "ЖЕСТКОЕ ДЫХАНИЕ"}
                          {:is-important "Нет",
                           :organ-system "Сердечно-сосудистая система",
                           :comment      "ПОВЫШЕНИЕ АРТЕРИАЛЬНОГО ДАВЛЕНИЯ 148/86"}],
       :location         "12",
       :has-deviations   "Да",
       :verified         true}
      (read-exp-from-file "vnok/PE.v2-v10.json"))))

(deftest vnok.PE.v2-v10-empty-test
  (is
    (=
      {:patient          {:name     "01-012",
                          :birthday "1953-06-14",
                          :gender   "Мужской"},
       :type             "vnok.PE.v2-v10",
       :is-done          true,
       :finished         false,
       :visit            "vnok/se.V9",
       :location         "01",
       :verified         false}
      (read-exp-from-file "vnok/PE.v2-v10-empty.json"))))

(deftest vnok.UV-test
  (is
    (=
      {:patient  {:name     "01-003",
                  :birthday "1944-02-10",
                  :gender   "Женский"},
       :date     "2018-07-02",
       :group    "1",
       :type     "vnok.UV",
       :finished true,
       :reason   "НЯ/СНЯ",
       :visit    "vnok/se.UV",
       :comment  "1",
       :location "01",
       :verified true}
      (read-exp-from-file "vnok/UV.json"))))

;; Check that we can parse 24h based date
(deftest parse-date-test
  (is (s/valid? ::c/exp
           {:patient {:name "01-005",
                      :birthday "1940-09-06",
                      :gender "Женский"},
            :date "2018-09-05",
            :age "77",
            :race "Европеоидная",
            :birthday "1940-09-06",
            :type "vnok.DEMO",
            :finished true,
            :visit "vnok/se.SCR",
            :agr-datetime "2018-09-05 16:36",
            :gender "Женский",
            :location "01",
            :verified true})))

(deftest spec-fail-test
  (is ((complement s/valid?)
        ::c/exp
        {:patient {:name "01-005",
                   :birthday "1940-09-06",
                   :gender "Женский"},
         :date 123,
         :age "77",
         :race "Европеоидная",
         :birthday "1940-09-06",
         :type "vnok.DEMO",
         :finished true,
         :visit "vnok/se.SCR",
         :agr-datetime "2018-09-05 16:36",
         :gender "Женский",
         :location "01",
         :verified true}))

  (is ((complement s/valid?)
        ::c/exp
        {:patient {:name "01-005",
                   :birthday "1940-09-06",
                   :gender "Женский"},
         :date "2019",
         :age "77",
         :race "Европеоидная",
         :birthday "1940-09-06",
         :type "vnok.DEMO",
         :finished true,
         :visit "vnok/se.SCR",
         :agr-datetime "2018-09-05 16:36",
         :gender nil,
         :location "01",
         :verified true}))

  (is ((complement s/valid?)
        {:patient {:name "01-002",
                   :birthday "1939-10-06",
                   :gender "Мужской"},
         :has-records "Да",
         :type "vnok.MD",
         :finished true,
         :records [{:status "Продолжается",
                    :condition "Артериальная гипертензия II стадии, риск 3, контролируемая.",
                    :start-date "2013"}
                   {:status "Продолжается",
                    :condition "Хроническая сердечная недостаточность I стадии, ФК 2",
                    :start-date "2013"}
                   {:status "Разрешилось",
                    :condition "Экстрасиcтолия желудочковая, редкая одиночная",
                    :stop-date "2018-05-28",
                    :start-date "2016-08-31"}
                   {:status "Продолжается",
                    :condition "Дислипидемия",
                    :start-date "2016-09-03"}
                   {:status "Продолжается",
                    :condition "Сахарный диабет 2 типа, ИЦУГ < 7,5%",
                    :start-date "2015"}
                   {:status "Продолжается",
                    :condition "Атеросклероз магистральных артерий головы (клинически)",
                    :start-date "2018-05-28"}
                   {:status "Продолжается",
                    :condition "Синдром позвоночно-подключичного обкрадывания слева (клинически)",
                    :start-date "2018-05-28"}
                   {:status "Разрешилось",
                    :condition "Миграция водителя ритма по предсердиям",
                    :stop-date "2018-05-28",
                    :start-date "!!!!!!>>>>wrong<<<<<"}
                   {:status "Разрешилось",
                    :condition "Замедление атриовентрикулярного проведения",
                    :stop-date "2018-05-28",
                    :start-date "2018-05-28"}
                   {:status "Разрешилось",
                    :condition "Перелом правой верхней конечности",
                    :stop-date "1980",
                    :start-date "1980"}
                   {:status "Разрешилось",
                    :condition "Аппендэктомия",
                    :stop-date "1966",
                    :start-date "1966"}
                   {:status "Разрешилось",
                    :condition "герниопластика паховой грыжи слева",
                    :stop-date "2009",
                    :start-date "2009"}
                   {:status "Продолжается",
                    :condition "Боль в левой ноге, вынуждающая остановиться при ходьбе около 100 метров",
                    :start-date "2017-10"}],
         :visit "vnok/se.SCR",
         :location "01",
         :verified true}
        (read-exp-from-file "vnok/MD.json"))))
