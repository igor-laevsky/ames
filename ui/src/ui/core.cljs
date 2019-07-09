(ns ^:figwheel-hooks ui.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]

            [ui.router :as router]
            [ui.events :as events]
            [ui.subs :as subs]))

(defn home-view []
  [:h1 [:a {:href (router/url-for :test)} "Home"]])

(defn test-view []
  [:h1 [:a {:href (router/url-for :test2 :id 123)} "Test"]])

(defn test2-view []
  [:h1 [:a {:href (router/url-for :home)} "Test 2"]])

(defn error-view []
  [:h1 "Error"])

(defn main-app []
  (let [active-page @(re-frame/subscribe [:active-page])]
    (case active-page
      :home [home-view]
      :test [test-view]
      :test2 [test2-view])))

(enable-console-print!)

(defn mount []
  (reagent/render [main-app] (.getElementById js/document "app")))

(defn ^:export main []
  (router/start!)
  (re-frame/dispatch-sync [:initialize-db])
  (mount))

(defn ^:after-load re-render []
  (re-frame/clear-subscription-cache!)
  (mount))
