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

(s/def ::db (s/keys ::opt [::cur-location ::cur-patient ::patients ::visits]))
(s/def ::db-with-patients (s/keys ::req [::cur-location ::patients]
                                  ::opt [::visits ::cur-patient]))

(def validate-db (utils/validate-db ::db))
(def validate-db-with-patients (utils/validate-db ::db-with-patients))
