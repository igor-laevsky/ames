(ns ui.common.router
  (:require [cljs.spec.alpha :as s]
            [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [re-frame.core :as re-frame]))

(def ^:private routes
  ["/" {"" :home
        "test" :test
        ["test2/" :id] :test2}])

(s/def ::active-page #{:home :test :test2})

(s/def ::db (s/keys :req [::active-page]))

(defn- validate-db-helper [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def validate-db (re-frame/after (partial validate-db-helper ::db)))

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
