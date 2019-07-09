(ns ui.events
  (:require [cljs.pprint :refer [pprint]]
            [re-frame.core :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]

            [ui.db :as db]
            [ui.router :as router]))

(def ^:const api-url "http://0.0.0.0")
(defn endpoint [& params]
  (clojure.string/join "/" (concat [api-url] params)))

(re-frame/reg-event-db
  :initialize-db
  [db/validate-db]
  (fn-traced [db _] db/default-db))

(re-frame/reg-event-fx
  :set-active-page
  [db/validate-db]
  (fn-traced [{:keys [db]} [_ {:keys [page params]}]]
    (let [new-db (assoc db :active-page page)]
      (case page
        :home {:db (assoc db :active-page page)
               :dispatch [:get-locations]}))))

(re-frame/reg-event-fx
  :get-locations
  [db/validate-db]
  (fn-traced [{:keys [db]} [_ {:keys [page params]}]]
    {:http-xhrio {:method          :get
                  :uri             (endpoint "locations")
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:get-locations-success]
                  :on-failure      [:api-request-error :get-locations]}}))

(re-frame/reg-event-fx
  :get-locations-success
  [db/validate-db]
  (fn-traced [cofx [_ result]]
    (pprint "Successuly got locations")
    (pprint result)
    {}))

(re-frame/reg-event-fx
  :api-request-error
  [db/validate-db]
  (fn-traced [cofx [_ reason result]]
    (pprint (str "Failed network request" reason))
    (pprint result)
    {}))
