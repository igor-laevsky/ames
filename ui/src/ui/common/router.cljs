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
        [true :not-found]]])

(s/def ::active-page #{:home :list-patients :list-visits :not-found})

(s/def ::db (s/keys :req [::active-page]))

(re-frame/reg-sub
  ::active-page
  (fn [db _] (::active-page db)))

(def ^:private pushy-router
  (let [dispatch #(re-frame/dispatch
                    [:set-active-page {:page (:handler %)
                                       :params (:route-params %)}])
        match #(bidi/match-route routes %)]
    (pushy/pushy dispatch match)))

(defn start! []
  (pushy/start! pushy-router))

(def url-for (partial bidi/path-for routes))
