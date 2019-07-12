(ns ui.locations.db
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :as re-frame]))

(s/def ::name string?)
(s/def ::total int?)
(s/def ::verified int?)
(s/def ::location (s/keys :req-un [::name ::total ::verified]))

(s/def ::locations (s/coll-of ::location))

(s/def ::db (s/keys :opt [::locations]))

(defn- validate-db-helper [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def validate-db (re-frame/after (partial validate-db-helper ::db)))
