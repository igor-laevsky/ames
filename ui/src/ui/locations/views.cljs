(ns ui.locations.views
  (:require [cljs.pprint :refer [pprint]]
            [re-frame.core :as re-frame]

            [ui.common.router :as router]
            [ui.locations.subs :as subs]))

;; Helper function which displays single locations as a card on a grid.
(defn location-view [location]
  ^{:key (:name location)}
  [:div.location.card.shadow-sm.m-2 {:style {:width "10rem"}}
   [:div.card-body
    [:h5.card-title.font-weight-bold "Центр " (:name location)]
    [:p.card-text
     "Проверено"
     [:br]
     [:span.text-success.font-weight-bold (:verified location)]
     " из "
     [:span.text-danger.font-weight-bold (:total location)]]
    [:a.stretched-link
     {:href (router/url-for :list-patients :location-name (:name location))}]]])

;; Lists all available locations into one big grid.
;;
(defn main []
  (let [locations @(re-frame/subscribe [::subs/locations])]
    [:div.row (for [loc locations] (location-view loc))]))
