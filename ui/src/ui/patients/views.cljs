(ns ui.patients.views
  (:require [cljs.pprint :refer [pprint]]
            [re-frame.core :as re-frame]

            [ui.common.router :as router]
            [ui.common.meta :as meta]
            [ui.patients.subs :as subs]))

;; Lists patients, expected to be used as a sidebar.
(defn patient-list []
  (let [patients @(re-frame/subscribe [::subs/patients])
        cur-location @(re-frame/subscribe [::subs/cur-location])
        cur-patient @(re-frame/subscribe [::subs/cur-patient])
        active-patient (->> patients
                            (filter #(= cur-patient (:name %)))
                            first)]
    [:ul.nav.flex-column
     [:p.sidebar-heading.d-flex.justify-content-between.align-items-center.px-3.mt-4.mb-1.text-muted
      [:a.d-flex.align-items-center.text-muted
       {:href (router/url-for :home)}
       [:img {:src "/icons/back.svg" :alt "Back"}]
       [:span.font-weight-bold "Назад"]]
      [:span.h6.font-weight-bold "Центр " cur-location]]

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
          [:span.text-danger (:total p)]]]])

     [:div.mt-4]]))

(defn visits-list []
  (let [visits @(re-frame/subscribe [::subs/visits])
        cur-location @(re-frame/subscribe [::subs/cur-location])
        cur-patient @(re-frame/subscribe [::subs/cur-patient])
        cur-visit @(re-frame/subscribe [::subs/cur-visit])
        active-visit (->> visits
                          (filter #(= cur-visit (select-keys % [:name :group])))
                          (first))]
    [:ul.nav.flex-column.nav-pills
     (for [v visits]
       ^{:key (meta/visit->name v)}
       [:li.nav-item
        [(if (identical? active-visit v) :a.nav-link.active :a.nav-link)
         {:href (router/url-for :list-exps
                                :location-name cur-location
                                :patient-name cur-patient
                                :visit-name (meta/visit->url-name v))}
         (meta/visit->name v)
         " "
         [:span.font-weight-bold
          [:span.text-success (:verified v)]
          " / "
          [:span.text-danger (:total v)]]]])
     [:div.mt-4]]))

(defn exp-list []
  (let [exps @(re-frame/subscribe [::subs/exps])
        cur-location @(re-frame/subscribe [::subs/cur-location])
        cur-patient @(re-frame/subscribe [::subs/cur-patient])
        cur-visit @(re-frame/subscribe [::subs/cur-visit])]
    [:table.table.table-hover
     [:thead
      [:tr
       [:th {:scope "col"} "Дата"]
       [:th {:scope "col"} "Название"]]]

     [:tbody
      (for [e exps]
        (let [exp-url (router/url-for :show-exp
                                      :location-name cur-location
                                      :patient-name cur-patient
                                      :visit-name (meta/visit->url-name cur-visit)
                                      :exp-id (:_id e))]
          ^{:key (:_id e)}
          [:tr
           [:td
            [:a
             {:href exp-url}
             (get-in e [:_source :date] "--")]]
           [:td
            [:a
             {:href exp-url}
             (get-in e [:_source :type] "--")]]]))]]))

(defn show-exp [exp-id]
  (let [exp @(re-frame/subscribe [::subs/exp-by-id exp-id])
        cur-location @(re-frame/subscribe [::subs/cur-location])
        cur-patient @(re-frame/subscribe [::subs/cur-patient])
        cur-visit @(re-frame/subscribe [::subs/cur-visit])]
    (if (not-any? nil? [exp cur-location cur-patient cur-visit])
      [:div
       [:a.h6.align-left
        {:href (router/url-for :list-exps
                               :location-name cur-location
                               :patient-name cur-patient
                               :visit-name (meta/visit->url-name cur-visit))}
        "< Назад"]
       [:h4
        (with-out-str (pprint (:_source exp)))]]
      (:h6 "Loading"))))
