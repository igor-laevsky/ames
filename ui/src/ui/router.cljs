(ns ui.router
  (:require [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [re-frame.core :as re-frame]))

(def ^:private routes
  ["/" {"" :home
        "test/" :test
        ["test2/" :id] :test2}])

(def ^:private pushy-router
  (let [dispatch #(re-frame/dispatch
                    [:set-active-page {:page (:handler %)
                                       :params {:route-params %}}])
        match #(bidi/match-route routes %)]
    (pushy/pushy dispatch match)))

(defn start! []
  (pushy/start! pushy-router))

(def url-for (partial bidi/path-for routes))
