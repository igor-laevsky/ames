(ns ui.locations.events
  (:require [cljs.pprint :refer [pprint]]
            [re-frame.core :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]

            [ui.common.config :as cfg]
            [ui.locations.db :as db]))

(re-frame/reg-event-fx
  :get-locations
  [db/validate-db]
  (fn-traced [{:keys [db]} [_ {:keys [page params]}]]
    {:http-xhrio {:method          :get
                  :uri             (cfg/endpoint "locations")
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:get-locations-success]
                  :on-failure      [:api-request-error :get-locations]}}))

(re-frame/reg-event-db
  :get-locations-success
  [db/validate-db]
  (fn-traced [db [_ result]]
    (assoc db ::db/locations result)))

(re-frame/reg-event-fx
  :api-request-error
  [db/validate-db]
  (fn-traced [cofx [_ reason result]]
    (pprint (str "Failed network request" reason))
    (pprint result)
    {}))
