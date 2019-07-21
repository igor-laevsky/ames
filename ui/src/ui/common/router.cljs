(ns ui.common.router
  (:require [cljs.spec.alpha :as s]
            [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [re-frame.core :as re-frame]

            [ui.common.utils :as utils]))

(def ^:private routes
  ["/" [["" :home]
        [[:location-name "/"] :list-patients]
        [[:location-name "/" :patient-name "/"] :list-visits]
        [[:location-name "/" :patient-name "/" :visit-name "/"] :list-exps]
        [[:location-name "/" :patient-name "/" :visit-name "/" :exp-id "/"] :show-exp]
        [true :not-found]]])

(s/def ::active-page
  #{:home :list-patients :list-visits :list-exps :show-exp :not-found})

(s/def ::page-params (s/or :nil nil? :map map?))
(s/def ::db (s/keys :req [::active-page]
                    :opt [::page-params]))

(re-frame/reg-sub
  ::active-page
  (fn [db _] (::active-page db)))

(re-frame/reg-sub
  ::page-params
  (fn [db _] (::page-params db)))

(def ^:private pushy-router
  (let [dispatch #(re-frame/dispatch
                    [:set-active-page {:page (:handler %)
                                       :params (:route-params %)}])
        match #(bidi/match-route routes %)]
    (pushy/pushy dispatch match)))

(defn start! []
  (pushy/start! pushy-router))

(def url-for (partial bidi/path-for routes))
