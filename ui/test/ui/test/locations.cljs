(ns ui.test.locations
  (:require [cljs.test :refer-macros [deftest is testing]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]

            [ui.locations.subs :as subs]
            [ui.locations.events :as events]))

(deftest get-locations-test
  (rf-test/run-test-async
    (rf/dispatch [:initialize-db])

    (let [locations (rf/subscribe [::subs/locations])]
      (is (empty? @locations))
      (rf/dispatch [::events/get-locations])
      (rf-test/wait-for [::events/get-locations-success ::events/api-request-error]
        (is (not= 0 (count @locations)))))))
