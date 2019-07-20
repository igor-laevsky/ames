(ns ui.patients.views
  (:require [cljs.pprint :refer [pprint]]
            [re-frame.core :as re-frame]

            [ui.common.router :as router]
            [ui.common.utils :as utils]
            [ui.patients.subs :as subs]))

;; Lists patients, expected to be used as a sidebar.
(defn patient-list []
  (let [patients @(re-frame/subscribe [::subs/patients])
        cur-location @(re-frame/subscribe [::subs/cur-location])
        cur-patient @(re-frame/subscribe [::subs/cur-patient])]
    [:ul.nav.flex-column
     [:p.sidebar-heading.d-flex.justify-content-between.align-items-center.px-3.mt-4.mb-1.text-muted
      [:a.d-flex.align-items-center.text-muted
       {:href (router/url-for :home)}
       [:img {:src "/icons/back.svg" :alt "Back"}]
       [:span.font-weight-bold "Назад"]]
      [:span.h6.font-weight-bold "Центр " cur-location]]

     (let [active-patient (->> patients
                               (filter #(= cur-patient (:name %)))
                               first)]
       (for [p patients]
         ^{:key (:name p)}
         [(if (identical? active-patient p) :li.nav-item.active :li.nav-item)
          [:a.nav-link
           {:href (router/url-for :list-visits
                                  :location-name cur-location
                                  :patient-name (:name p))}
           [:img {:src "/icons/human.svg"}]
           " "
           (:name p) " " (get p :rand-num "")
           " "
           [:span.font-weight-bold
            [:span.text-success (:verified p)]
            " / "
            [:span.text-danger (:total p)]]]]))

     [:div.mt-4]]))

(defn visits-list []
  (let [visits @(re-frame/subscribe [::subs/visits])]
    [:ul.nav.flex-column.nav-pills
     (for [v visits]
       ^{:key (:name v)}
       [:li.nav-item
        [:a.nav-link
         {:href "#"}
         (utils/visit->name v)
         " "
         [:span.font-weight-bold
          [:span.text-success (:verified v)]
          " / "
          [:span.text-danger (:total v)]]]])
     [:div.mt-4]]))
