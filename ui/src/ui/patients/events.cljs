(ns ui.patients.events
  (:require [cljs.pprint :refer [pprint]]
            [re-frame.core :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]

            [ui.common.config :as cfg]
            [ui.patients.db :as db]))

(re-frame/reg-event-fx
  ::get-patients
  [db/validate-db]
  (fn-traced [{:keys [db]} [_ {:keys [location-id]}]]
    {:http-xhrio {:method          :get
                  :uri             (cfg/endpoint "patients")
                  :params          {:loc location-id}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::get-patients-success]
                  :on-failure      [::get-patients-fail]}}))

(re-frame/reg-event-db
  ::get-patients-success
  [db/validate-db]
  (fn-traced [db [_ result]]
    (assoc db ::db/patients result)))

(re-frame/reg-event-fx
  ::get-patients-fail
  [db/validate-db]
  (fn-traced [cofx [_ reason result]]
    (pprint (str "Failed network request" reason))
    (pprint result)
    (js/alert "Api request error: unable to get patients list")
    {}))
