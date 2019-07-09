(ns ui.test.example
  (:require [cljs.test :refer-macros [deftest is testing]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]

            [ui.events :as events]
            [ui.db :as db]
            [ui.subs :as subs]))

(deftest set-active-page-test
  (rf-test/run-test-sync
    (rf/dispatch [:initialize-db])

    (let [cur-page (rf/subscribe [:active-page])]
      (is (= :home @cur-page))
      (rf/dispatch [:set-active-page {:page :test}])
      (is (= :test @cur-page)))))
