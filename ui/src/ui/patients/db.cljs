(ns ui.patients.db
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :as re-frame]

            [ui.common.utils :as utils]))

(s/def ::cur-location string?)
(s/def ::cur-patient string?)

(s/def ::name string?)
(s/def ::rand-num string?)
(s/def ::total int?)
(s/def ::verified int?)

(s/def ::patient (s/keys :req-un [::name ::total ::verified]
                         ::opt-un [::rand-num ]))
(s/def ::patients (s/coll-of ::patient))

(s/def ::visit (s/keys :req-un [::name ::total ::verified]))
(s/def ::visits (s/coll-of ::visit))

(s/def :ui.patients.db.exp/type string?)
(s/def :ui.patients.db.exp/visit string?)
(s/def :ui.patients.db.exp/group string?)
(s/def :ui.patients.db.exp/location string?)
(s/def :ui.patients.db.exp/finished boolean?)
(s/def :ui.patients.db.exp/verified boolean?)
(s/def ::exp (s/keys :req-un [:ui.patients.db.exp/type
                              :ui.patients.db.exp/visit
                              :ui.patients.db.exp/group
                              :ui.patients.db.exp/location
                              :ui.patients.db.exp/finished
                              :ui.patients.db.exp/is-verified]))
(s/def ::exps (s/coll-of ::exp))

(s/def ::db (s/keys)) ; everything is optional
(s/def ::db-with-patients (s/keys ::req [::cur-location ::patients]))
(s/def ::db-with-visits (s/keys ::req [::cur-patient ::visits]))

(def validate-db (utils/validate-db ::db))
(def validate-db-with-patients (utils/validate-db ::db-with-patients))
(def validate-db-with-visits (utils/validate-db ::db-with-visits))
