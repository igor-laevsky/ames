(ns ui.common.net
  (:require
    [cljs.spec.alpha :as s]
    [re-frame.core :as re-frame]
    [day8.re-frame.http-fx :as httpfx]
    [day8.re-frame.tracing :refer-macros [fn-traced]]
    [ajax.core :as ajax]

    [ui.common.utils :as utils]))

;;; This namespace contains utility function to handle AJAX requests and
;;; track their progress.

(s/def ::loading-tags (s/map-of keyword? nat-int?))
(s/def ::db (s/keys :opt [::loading-tags]))
(s/def ::db-with-tags (s/keys :req [::loading-tags]))

(re-frame/reg-sub
  ::loading-tags
  (fn [db _] (::loading-tags db)))

(re-frame/reg-sub
  ::is-loading-tag
  :<- [::loading-tags]
  (fn [loading-tags [_ tag]]
    (assert (keyword? tag) "tag must be a keyword")
    (pos? (get loading-tags tag 0))))

(re-frame/reg-sub
  ::is-loading-anything
  :<- [::loading-tags]
  (fn [loading-tags _]
    (->> (vals loading-tags)
         (some pos?))))

(re-frame/reg-event-db
  ::request-start
  [(utils/validate-db ::db)]
  (fn-traced [db [_ tag]] (update-in db [::loading-tags tag] inc)))

(re-frame/reg-event-fx
  ::request-complete
  [(utils/validate-db ::db-with-tags)]
  (fn-traced [{:keys [db]} [_ tag cont result]]
    (let [events (->> (if (sequential? (first cont)) cont (vector cont))
                      (map #(conj % result)))]
      {:db (update-in db [::loading-tags tag] dec)
       :dispatch-n events})))

(s/def ::method #{:get :post :put :delete})
(s/def ::uri string?)
(s/def ::tag keyword?)
(s/def ::params map?)
(s/def ::on-success (s/or :seq sequential? :kw keyword?))
(s/def ::on-failure (s/or :seq sequential? :kw keyword?))
(s/def ::request-params
  (s/keys :req-un [::method ::uri ::tag ::on-success ::on-failure]
          :opt-in [::params]))

(defn ajax-request [params]
  (assert (s/valid? ::request-params params) (s/explain-str ::request-params params))
  (re-frame/dispatch [::request-start (:tag params)])
  (httpfx/http-effect
    (-> params
        (dissoc :tag :on-success :on-failure)
        (merge {:response-format (ajax/json-response-format {:keywords? true})
                :timeout 15000
                :on-success [::request-complete (:tag params) (:on-success params)]
                :on-failure [::request-complete (:tag params) (:on-failure params)]}))))

(re-frame/reg-fx :ajax ajax-request)
