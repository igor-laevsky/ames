(ns ui.locations.views
  (:require [cljs.pprint :refer [pprint]]
            [re-frame.core :as re-frame]

            [ui.patients.subs :as subs]))

;; Lists patients, expected to be used as a sidebar.
(defn patient-list [] true)