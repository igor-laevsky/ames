(ns ui.patients.subs
  (:require [re-frame.core :as re-frame]

            [ui.patients.db :as db]))

(re-frame/reg-sub
  ::patients
  (fn [db _] (sort-by :name (::db/patients db))))

(re-frame/reg-sub
  ::cur-location
  (fn [db _] (::db/cur-location db)))
