(ns ui.test.patients
  (:require [cljs.test :refer-macros [deftest is testing]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]

            [ui.patients.subs :as subs]
            [ui.patients.events :as events]))

(defn test-load-event [navs check-location check-patient check-visit]
  (rf-test/run-test-async
    (rf/dispatch-sync [:initialize-db])

    (let [patients (rf/subscribe [::subs/patients])
          visits (rf/subscribe [::subs/visits])
          exps (rf/subscribe [::subs/exps])
          cur-location (rf/subscribe [::subs/cur-location])
          cur-patient (rf/subscribe [::subs/cur-patient])
          cur-visit (rf/subscribe [::subs/cur-visit])]
      (is (empty? @patients))
      (is (empty? @visits))
      (is (empty? @exps))
      (rf/dispatch [::events/load navs])
      (rf-test/wait-for [::events/set-cur-location]
        (is (check-location @cur-location))
        (rf-test/wait-for [::events/set-cur-patient]
          (is (check-patient @cur-patient @patients))
          (rf-test/wait-for [::events/set-cur-visit]
            (is (check-visit @cur-visit @visits))))))))

(deftest load-from-location
  (test-load-event
    {:location-name "01"}
    #(= % "01")
    (fn [cur-patient patients] (= cur-patient (-> patients first :name)))
    (fn [cur-visit visits] (= cur-visit (-> visits first (select-keys [:name :group]))))))

(deftest load-from-location-patient
  (test-load-event
    {:location-name "01" :patient-name "01-001"}
    #(= % "01")
    (fn [cur-patient _] (= cur-patient "01-001"))
    (fn [cur-visit visits] (= cur-visit (-> visits first (select-keys [:name :group]))))))

(deftest load-from-location-patient-visit
  (test-load-event
    {:location-name "01" :patient-name "01-003" :visit-name "UV-1"}
    #(= % "01")
    (fn [cur-patient _] (= cur-patient "01-003"))
    (fn [cur-visit _]
      (do
        (print cur-visit)
        (= cur-visit {:name "vnok/se.UV" :group "1"})))))
