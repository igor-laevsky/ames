(ns ^:figwheel-hooks ui.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]

            [ui.router :as router]))

(defn example-view []
  [:h1 [:a {:href "/test"} "Example  !"]])

(defn mount []
  (reagent/render [example-view] (.getElementById js/document "app")))

(defn ^:export main []
  (router/start!)
  (mount))

(defn ^:after-load re-render []
  (enable-console-print!)
  (re-frame/clear-subscription-cache!)
  (mount))
