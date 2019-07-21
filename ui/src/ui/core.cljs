(ns ^:figwheel-hooks ui.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]

            [ui.common.router :as router]
            [ui.common.utils :as utils]

            [ui.locations.db]
            [ui.locations.events]
            [ui.locations.subs]
            [ui.locations.views]

            [ui.patients.db]
            [ui.patients.events]
            [ui.patients.subs]
            [ui.patients.views]))

(re-frame/reg-event-db
  :initialize-db
  (fn-traced [db _] {::router/active-page :home}))

(re-frame/reg-event-fx
  :set-active-page
  [(utils/validate-db ::router/db)]
  (fn-traced [{:keys [db]} [_ {:keys [page params]}]]
    (let [new-db (assoc db ::router/active-page page)]
      (case page
        ; Home page is 'locations'
        :home {:db new-db
               :dispatch [::ui.locations.events/get-locations]}

        (:list-patients :list-visits :list-exps)
          {:db new-db
           :dispatch [::ui.patients.events/load params]}

        ; default, not found
        {:db (assoc db ::router/active-page :not-found)}))))

(defn main-app []
  (let [active-page @(re-frame/subscribe [::router/active-page])]
    (case active-page
      :home [ui.locations.views/main]

      (:list-patients :list-visits :list-exps)
      [:div.row
       [:nav.col-md-2.bg-light.sidebar
        [ui.patients.views/patient-list]]
       [:nav.col-md-2.bg-light.sidebar-second
        [ui.patients.views/visits-list]]
       [:main.col-md-6.ml-sm-auto.col-lg-80.px-4
        [ui.patients.views/exp-list]]]

      :not-found [:h1 "Error 404"])))

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
