(ns ui.patients.db
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :as re-frame]

            [ui.common.utils :as utils]))

(s/def ::name string?)
(s/def ::rand-num string?)
(s/def ::total int?)
(s/def ::verified int?)
(s/def ::patient (s/keys :req-un [::name ::rand-num ::total ::verified]))

(s/def ::patients (s/coll-of ::patient))

(s/def ::db (s/keys ::opt [::patients]))

(def validate-db (utils/validate-db ::db))
