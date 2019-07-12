(ns ui.locations.subs
  (:require [re-frame.core :as re-frame]

            [ui.locations.db :as db]))

(re-frame/reg-sub
  ::locations
  (fn [db _] (::db/locations db)))
