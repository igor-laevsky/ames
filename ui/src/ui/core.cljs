(ns ^:figwheel-hooks ui.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]))

(defn example-view []
  [:h1 "Example wold !"])

(defn mount []
  (reagent/render [example-view] (.getElementById js/document "app")))

(defn ^:export main []
  (mount))

(defn ^:after-load re-render []
  (enable-console-print!)
  (re-frame/clear-subscription-cache!)
  (mount))
