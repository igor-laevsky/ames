(ns ^:figwheel-hooks ui.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]

            [ui.router :as router]
            [ui.events :as events]
            [ui.subs :as subs]

            [ui.locations.db]
            [ui.locations.events]
            [ui.locations.subs]
            [ui.locations.views]))

(defn main-app []
  (let [active-page @(re-frame/subscribe [:active-page])]
    (case active-page
      :home [ui.locations.views/main])))

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
