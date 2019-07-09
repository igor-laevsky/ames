(ns ui.events
  (:require [re-frame.core :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]

            [ui.db :as db]
            [ui.router :as router]))

(re-frame/reg-event-db
  :initialize-db

  [db/validate-db]
  (fn-traced [db _] db/default-db))

(re-frame/reg-event-db
  :set-active-page
  (fn-traced [db [_ {:keys [page params]}]]
    (-> db
        (assoc :active-page page)
        (assoc :params params))))
