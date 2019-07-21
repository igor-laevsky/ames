(ns ui.patients.events
  (:require [cljs.pprint :refer [pprint]]
            [re-frame.core :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [day8.re-frame.http-fx]
            [day8.re-frame.async-flow-fx]
            [ajax.core :as ajax]

            [ui.common.config :as cfg]
            [ui.common.meta :as meta]
            [ui.patients.db :as db]))

;; Load all the required data. location-name is required. Two other params are
;; optional. If not specified we will pick some patient and some visit.
;;
(re-frame/reg-event-fx
  ::load
  [db/validate-db]
  (fn-traced [{:keys [db]} [_ {:keys [location-name patient-name visit-name] :as args}]]
    {:async-flow
     {:first-dispatch [::get-patients args]
      :rules
      [{:when :seen? :events ::get-patients-success :dispatch [::get-visits args]}
       {:when :seen? :events ::get-visits-success :dispatch [::get-exps args]}]}}))

;; Requests list of patients from server. 'location-name' is required.
;;
(re-frame/reg-event-fx
  ::get-patients
  [db/validate-db]
  (fn-traced [{:keys [db]} [_ {:keys [location-name]}]]
    {:http-xhrio {:method          :get
                  :uri             (cfg/endpoint "patients")
                  :params          {:loc location-name}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::get-patients-success]
                  :on-failure      [::get-patients-fail]}
     :db (assoc db ::db/cur-location location-name)}))

(re-frame/reg-event-db
  ::get-patients-success
  [db/validate-db]
  (fn-traced [db [_ result]]
    (assoc db ::db/patients (sort-by :name result))))

(re-frame/reg-event-fx
  ::get-patients-fail
  [db/validate-db]
  (fn-traced [cofx [_ reason result]]
    (pprint (str "Failed network request" reason))
    (pprint result)
    (js/alert "Api request error: unable to get patients list")
    {}))

;; Requests list of visits for a given patient from server.
;; 'patient-name' is optional, will pick some unspecified patient if absent.
;; Should be called after patient list is loaded.
;;
(re-frame/reg-event-fx
  ::get-visits
  [db/validate-db-with-patients]
  (fn-traced [{:keys [db]} [_ {:keys [patient-name]}]]
    (let [location-name (-> db ::db/cur-location)
          clean-patient-name (if-not patient-name
                               (-> db ::db/patients first :name)
                               patient-name)]
      {:http-xhrio {:method :get
                    :uri (cfg/endpoint "visits")
                    :params {:loc location-name
                             :pat clean-patient-name}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [::get-visits-success]
                    :on-failure [::get-visits-fail]}
       :db (assoc db ::db/cur-patient clean-patient-name)})))

(re-frame/reg-event-db
  ::get-visits-success
  [db/validate-db]
  (fn-traced [db [_ result]]
    (assoc db ::db/visits (sort-by :name result))))

(re-frame/reg-event-fx
  ::get-visits-fail
  [db/validate-db]
  (fn-traced [cofx [_ reason result]]
    (pprint (str "Failed network request" reason))
    (pprint result)
    (js/alert "Api request error: unable to get visits list")
    {}))

;; Request list of exps for the given patient and visit.
;;
(re-frame/reg-event-fx
  ::get-exps
  [db/validate-db-with-visits]
  (fn-traced [{:keys [db]} [_ {:keys [visit-name]}]]
    (let [location-name (-> db ::db/cur-location)
          patient-name (-> db ::db/cur-patient)
          clean-visit (if-not visit-name
                        (-> db ::db/visits first (select-keys [:name :group]))
                        (meta/url-name->visit visit-name))
          query (if (:group clean-visit)
                  (goog.string/format
                    "location:%s AND patient.name:%s AND visit:%s AND group:%s",
                    location-name patient-name (:name clean-visit) (:group clean-visit))
                  (goog.string/format
                    "location:%s AND patient.name:%s AND visit:%s",
                    location-name patient-name (:name clean-visit)))]
      {:http-xhrio {:method :get
                    :uri (cfg/endpoint "search")
                    :params {:q query}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [::get-exps-success]
                    :on-failure [::get-exps-fail]}
       :db (assoc db ::db/cur-visit clean-visit)})))

(re-frame/reg-event-db
  ::get-exps-success
  [db/validate-db]
  (fn-traced [db [_ result]]
    (assoc db ::db/exps (:hits result))))

(re-frame/reg-event-fx
  ::get-exps-fail
  [db/validate-db]
  (fn-traced [cofx [_ reason result]]
    (pprint (str "Failed network request" reason))
    (pprint result)
    (js/alert "Api request error: unable to get exp list")
    {}))
