(ns ui.db
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :as re-frame]

            [ui.router :as router]))

(s/def ::active-page router/route-ids)

(s/def ::name string?)
(s/def ::total int?)
(s/def ::verified int?)
(s/def ::location (s/keys :req-un [::name ::total ::verified]))
(s/def ::locations (s/coll-of ::location))

(s/def ::db (s/keys :req-un [::active-page]
                    :opt-un [::locations]))

(def default-db {:active-page :home})

(defn- validate-db-helper [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def validate-db (re-frame/after (partial validate-db-helper ::db)))
