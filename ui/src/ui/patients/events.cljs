(ns ui.patients.events
  (:require [clojure.spec.alpha :as s]
            [cljs.pprint :refer [pprint]]
            [re-frame.core :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [day8.re-frame.http-fx]
            [day8.re-frame.async-flow-fx]
            [ajax.core :as ajax]

            [ui.common.net]
            [ui.common.config :as cfg]
            [ui.common.meta :as meta]
            [ui.patients.db :as db]))

;; Load all of the data we need to render the page.
;; location-name is required. Two other params are optional. If not specified
;; we will pick some patient and some visit.
(re-frame/reg-event-fx
  ::load
  [db/validate-db]
  (fn
    [{:keys [db]} [_ {:keys [location-name patient-name visit-name] :as navs}]]

    (assert location-name "Must provide location name")
    (let [request-rules
          [{:when :seen? :events ::set-cur-location :dispatch [::get-patients]}
           {:when :seen? :events ::set-cur-patient :dispatch [::get-visits]}
           {:when :seen? :events ::set-cur-visit :dispatch [::get-exps]}
           {:when :seen-any-of?
            :events [::get-patients-fail
                     ::get-visits-fail
                     ::get-exps-fail]
            :halt? true}
           {:when :seen-all-of?
            :events [::get-patients-success
                     ::get-visits-success
                     ::get-exps-success]
            :halt? true}]

          nav-rules
          (cond->
            []
            (nil? patient-name) (conj {:when :seen?
                                       :events ::get-patients-success
                                       :dispatch [::set-default-patient]})
            (nil? visit-name) (conj {:when :seen?
                                     :events ::get-visits-success
                                     :dispatch [::set-default-visit]}))]

      {:async-flow
       {:first-dispatch [::set-navigation navs]
        :rules (concat request-rules nav-rules)}})))

;;
;; Navigation events
;;

(re-frame/reg-event-db
  ::set-cur-location
  [db/validate-db]
  (fn-traced [db [_ new-location]]
    (if (s/valid? ::db/cur-location new-location)
      (assoc db ::db/cur-location new-location)
      (throw (ex-info "Unable to set current location" {:data new-location})))))

(re-frame/reg-event-db
  ::set-cur-patient
  [db/validate-db]
  (fn-traced [db [_ new-patient]]
    (if (s/valid? ::db/cur-patient new-patient)
      (assoc db ::db/cur-patient new-patient)
      (throw (ex-info "Unable to set current patient" {:data new-patient})))))

(re-frame/reg-event-db
  ::set-cur-visit
  [db/validate-db]
  (fn-traced [db [_ new-visit]]
    (if (s/valid? ::db/cur-visit new-visit)
      (assoc db ::db/cur-visit new-visit)
      (throw (ex-info "Unable to set current visit" {:data new-visit})))))

(re-frame/reg-event-fx
  ::set-default-patient
  [db/validate-db-with-patients]
  (fn-traced [{:keys [db]} _]
    {:dispatch [::set-cur-patient (-> db ::db/patients first :name)]}))

(re-frame/reg-event-fx
  ::set-default-visit
  [db/validate-db-with-visits]
  (fn-traced [{:keys [db]} _]
    {:dispatch [::set-cur-visit
                (-> db ::db/visits first (select-keys [:name :group]))]}))

;; Conditionally dispatches to set all of the navigation variables.
(re-frame/reg-event-fx
  ::set-navigation
  [db/validate-db]
  (fn [{:keys [db]} [_ {:keys [location-name patient-name visit-name]}]]
    {:dispatch-n
     (cond->
       []
       location-name (conj [::set-cur-location location-name])
       patient-name (conj [::set-cur-patient patient-name])
       visit-name (conj [::set-cur-visit (meta/url-name->visit visit-name)]))}))

;;
;; API network requests for various parts of experimental data.
;;

;; Requests list of patients from server for the current location.
(re-frame/reg-event-fx
  ::get-patients
  [db/validate-db]
  (fn-traced [{:keys [db]} _]
    {:ajax {:tag :patients
            :method :get
            :uri (cfg/endpoint "patients")
            :params {:loc (::db/cur-location db)}
            :on-success [::get-patients-success]
            :on-failure [::get-patients-fail]}}))

(re-frame/reg-event-db
  ::get-patients-success
  [db/validate-db]
  (fn-traced [db [_ result]]
    (assoc db ::db/patients (meta/sort-patients result))))

(re-frame/reg-event-fx
  ::get-patients-fail
  [db/validate-db]
  (fn-traced [cofx [_ reason result]]
    (pprint (str "Failed network request" reason))
    (pprint result)
    (js/alert "Api request error: unable to get patients list")
    {}))

;; Requests list of visits for a current patient.
(re-frame/reg-event-fx
  ::get-visits
  [db/validate-db-with-patients]
  (fn-traced [{:keys [db]} _]
      {:ajax {:tag :patients
              :method :get
              :uri (cfg/endpoint "visits")
              :params {:loc (::db/cur-location db)
                       :pat (::db/cur-patient db)}
              :on-success [::get-visits-success]
              :on-failure [::get-visits-fail]}}))

(re-frame/reg-event-db
  ::get-visits-success
  [db/validate-db]
  (fn-traced [db [_ result]]
    (assoc db ::db/visits (meta/sort-visits result))))

(re-frame/reg-event-fx
  ::get-visits-fail
  [db/validate-db]
  (fn-traced [cofx [_ reason result]]
    (pprint (str "Failed network request" reason))
    (pprint result)
    (js/alert "Api request error: unable to get visits list")
    {}))

;; Request list of exps for the current patient and visit.
(re-frame/reg-event-fx
  ::get-exps
  [db/validate-db-with-visits]
  (fn-traced [{:keys [db]} _]
    (let [location-name (::db/cur-location db)
          patient-name (::db/cur-patient db)
          cur-visit (::db/cur-visit db)
          query (if (:group cur-visit)
                  (goog.string/format
                    "location:%s AND patient.name:%s AND visit:%s AND group:%s",
                    location-name patient-name (:name cur-visit) (:group cur-visit))
                  (goog.string/format
                    "location:%s AND patient.name:%s AND visit:%s",
                    location-name patient-name (:name cur-visit)))]
      {:ajax {:tag :patients
              :method :get
              :uri (cfg/endpoint "search")
              :params {:q query}
              :on-success [::get-exps-success]
              :on-failure [::get-exps-fail]}})))

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
