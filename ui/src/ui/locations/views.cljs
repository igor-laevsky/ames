(ns ui.locations.views
  (:require [cljs.pprint :refer [pprint]]
            [re-frame.core :as re-frame]

            [ui.locations.subs :as subs]))

(defn main []
  (let [locations @(re-frame/subscribe [::subs/locations])]
    [:h1 "Num locations is " (count locations)]))
