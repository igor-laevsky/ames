(ns ui.test.patients
  (:require [cljs.test :refer-macros [deftest is testing]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]

            [ui.patients.subs :as subs]
            [ui.patients.events :as events]))

(deftest get-patients-test
  (rf-test/run-test-async
    (rf/dispatch [:initialize-db])

    (let [patients (rf/subscribe [::subs/patients])]
      (is (empty? @patients))
      (rf/dispatch [::events/get-patients {:location-id "04"}])
      (rf-test/wait-for [::events/get-patients-success ::events/get-patients-fail]
        (is ((complement empty?) @patients))
        (is (not= 0 (count @patients)))))))
