(ns ui.patients.subs
  (:require [re-frame.core :as re-frame]

            [ui.patients.db :as db]))

(re-frame/reg-sub
  ::cur-location
  (fn [db _] (::db/cur-location db)))

(re-frame/reg-sub
  ::cur-patient
  (fn [db _] (::db/cur-patient db)))

(re-frame/reg-sub
  ::cur-visit
  (fn [db _] (::db/cur-visit db)))

(re-frame/reg-sub
  ::patients
  (fn [db _] (::db/patients db)))

(re-frame/reg-sub
  ::visits
  (fn [db _] (::db/visits db)))

(re-frame/reg-sub
  ::exps
  (fn [db _] (::db/exps db)))
