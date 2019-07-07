(ns ui.subs
  (:require [re-frame.core :as  re-frame]

            [ui.db :as db]))

(re-frame/reg-sub
  :active-page
  (fn [db _] (:active-page db)))
