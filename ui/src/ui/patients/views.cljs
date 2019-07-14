(ns ui.patients.views
  (:require [cljs.pprint :refer [pprint]]
            [re-frame.core :as re-frame]

            [ui.common.router :as router]
            [ui.patients.subs :as subs]))

;; Lists patients, expected to be used as a sidebar.
(defn patient-list []
  (let [patients @(re-frame/subscribe [::subs/patients])
        cur-location @(re-frame/subscribe [::subs/cur-location])]
    [:ul.nav.flex-column
     [:p.sidebar-heading.d-flex.justify-content-between.align-items-center.px-3.mt-4.mb-1.text-muted
      [:a.d-flex.align-items-center.text-muted
       {:href (router/url-for :home)}
       [:img {:src "/icons/back.svg" :alt "Back"}]
       [:span.font-weight-bold "Назад"]]
      [:h6.font-weight-bold "Центр " cur-location]]

     (for [p patients]
       ^{:key (:name p)}
       [:li.nav-item
        [:a.nav-link {:href "#"}
         [:img {:src "/icons/human.svg"}]
         " "
         (:name p) " " (get p :rand-num "")
         " "
         [:span.font-weight-bold
          [:span.text-success (:verified p)]
          " / "
          [:span.text-danger (:total p)]]]])

     [:div.mt-4]]))
