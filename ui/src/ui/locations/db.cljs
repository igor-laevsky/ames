(ns ui.locations.db
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :as re-frame]

            [ui.common.utils :as utils]))

(s/def ::name string?)
(s/def ::total int?)
(s/def ::verified int?)
(s/def ::location (s/keys :req-un [::name ::total ::verified]))

(s/def ::locations (s/coll-of ::location))

(s/def ::db (s/keys :opt [::locations]))

(def validate-db (utils/validate-db ::db))
