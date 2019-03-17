(ns cdl.vnok
  (:require [cdl.common :as c]
            [clojure.spec.alpha :as s]
            [json-path :as jp]
            [clojure.test :as test]
            [clojure.string :as string]))

(def exp-types (->>
                 ["DEMO" "MH" "MD" "PE.v1" "PE.v2-v10" "UV" "VS" "AE"]
                 (map (partial str "vnok/"))
                 (set)))

(def visit-types (->>
                   ["se.SCR" "se.SCR1" "se.V1" "se.V2" "se.V3" "se.V4" "se.V5"
                    "se.V6" "se.V7" "se.V8" "se.V9" "se.V10" "se.V11" "se.V12"
                    "se.UV" "se.CM" "se.PR" "se.AE" "se.DV"]
                   (map (partial str "vnok/"))
                   (set)))

; Refined common spec to be used in all of the vnok experiments.
(s/def ::type exp-types)
(s/def ::visit visit-types)
(s/def ::location (set (map (partial format "%02d") (range 1 19))))
(s/def ::vnok-common
  (s/merge ::c/exp-common
           (s/keys :req-un [::type ::visit ::location])))

; Extracts data common to all experiments
(defn parser-common [inp]
  {:visit    (str "vnok/" (jp/at-path "$.d.StudyEventOID" inp))
   :location (subs (jp/at-path "$.d.LocationOID" inp) 1)
   :group    (str (jp/at-path "$.d.StudyEventRepeatKey" inp))
   :finished (boolean (#{6 8} (jp/at-path "$.d.State" inp)))
   :verified (boolean (#{7 8} (jp/at-path "$.d.State" inp)))

   :patient  {:name     (jp/at-path "$.d.SubjectKey" inp)
              :birthday (jp/at-path "$.d.SubjectBrthDate" inp)
              :gender   (c/gender-decode (jp/at-path "$.d.SubjectSex" inp))
              :rand-num (str (jp/at-path "$.context.rand-num" inp))}})

; Screening visit demographic data (DEMO)
(s/def :vnok.DEMO/date ::c/date-time-str-or-nil)
(s/def :vnok.DEMO/agr-datetime ::c/date-time-str-or-nil)
(s/def :vnok.DEMO/birthday ::c/date-time-str-or-nil)
(s/def :vnok.DEMO/age ::c/age)
(s/def :vnok.DEMO/gender ::c/gender)
(s/def :vnok.DEMO/race ::c/race)
(s/def :vnok.DEMO/other string?)

(defmethod c/get-exp-spec "vnok/DEMO" [_]
  (s/merge
    ::vnok-common
    (s/keys :req-un
            [:vnok.DEMO/date
             :vnok.DEMO/agr-datetime
             :vnok.DEMO/birthday
             :vnok.DEMO/age
             :vnok.DEMO/gender
             :vnok.DEMO/race
             :vnok.DEMO/other])))

(defmethod c/parse-exp-from-json 264 [inp]
  (merge
    (parser-common inp)
    {:type "vnok/DEMO"
     :date
           (jp/at-path
             "$.d.FormData.SectionList[0].ItemGroupList[0].RowList[0].ItemList[1].Value"
             inp)

     :agr-datetime
           (string/trim
             (str
               (jp/at-path
                 "$.
                 d.FormData.SectionList[1].ItemGroupList[0].RowList[0].ItemList[1].Value"
                 inp)
               " "
               (jp/at-path
                 "$.d.FormData.SectionList[1].ItemGroupList[0].RowList[1].ItemList[1].Value"
                 inp)))

     :birthday
           (jp/at-path
             "$.d.FormData.SectionList[2].ItemGroupList[0].RowList[0].ItemList[1].Value"
             inp)

     :age
           (jp/at-path
             "$.d.FormData.SectionList[2].ItemGroupList[0].RowList[1].ItemList[1].Value"
             inp)

     :gender
           (c/gender-decode
             (jp/at-path
               "$.d.FormData.SectionList[2].ItemGroupList[0].RowList[2].ItemList[1].Value"
               inp))

     :race
           (c/race-decode
             (jp/at-path
               "$.d.FormData.SectionList[2].ItemGroupList[0].RowList[4].ItemList[1].Value"
               inp))

     :other
           (jp/at-path
             "$.d.FormData.SectionList[2].ItemGroupList[0].RowList[5].ItemList[1].Value"
             inp)
     }))

; Screening visit medical history (MD)
(s/def :vnok.MD/has-records ::c/yes-no)
(s/def :vnok.MD/condition string?)
(s/def :vnok.MD/start-date ::c/date-time-str-or-nil)
(s/def :vnok.MD/status ::c/status)
(s/def :vnok.MD/stop-date ::c/date-time-str-or-nil)
(s/def :vnok.MD/records
  (s/coll-of
    (s/keys :req-un [:vnok.MD/condition :vnok.MD/start-date
                     :vnok.MD/status :vnok.MD/stop-date])))

(defmethod c/get-exp-spec "vnok/MD" [_]
  (s/merge
    ::vnok-common
    (s/keys :req-un
            [:vnok.MD/has-records
             :vnok.MD/records])))

(defmethod c/parse-exp-from-json 205 [inp]
  (merge
    (parser-common inp)
    {:type "vnok/MD"
     :has-records
           (c/yes-no-decode
             (jp/at-path
               "$.d.FormData.SectionList[0].ItemGroupList[0].RowList[0].ItemList[1].Value"
               inp))
     :records
           (mapv
             #(hash-map
                :condition (jp/at-path "$.RowList[0].ItemList[1].Value" %)
                :start-date (jp/at-path "$.RowList[1].ItemList[1].Value" %)
                :status (c/status-decode (jp/at-path "$.RowList[2].ItemList[1].Value" %))
                :stop-date (jp/at-path "$.RowList[4].ItemList[1].Value" %))
             (jp/at-path "$.d.FormData.SectionList[2].ItemGroupList" inp))
     }))

; V2-V10 Physical examination (PE.v2-v10)

(s/def :vnok.PE.v1-v10/date ::c/date-time-str-or-nil)
(s/def :vnok.PE.v1-v10/examination-date ::c/date-time-str-or-nil)
(s/def :vnok.PE.v1-v10/is-done boolean?)
(s/def :vnok.PE.v1-v10/not-done-reason string?)
(s/def :vnok.PE.v1-v10/has-deviations ::c/yes-no)

(s/def :vnok.PE.v1-v10/organ-system ::c/organ-system)
(s/def :vnok.PE.v1-v10/is-important ::c/yes-no)
(s/def :vnok.PE.v1-v10/comment string?)
(s/def :vnok.PE.v1-v10/deviations
  (s/coll-of
    (s/keys :req-un [:vnok.PE.v1-v10/organ-system
                     :vnok.PE.v1-v10/is-important
                     :vnok.PE.v1-v10/comment])))

(defmethod c/get-exp-spec "vnok/PE.v2-v10" [_]
  (s/merge
    ::vnok-common
    (s/keys :req-un [:vnok.PE.v1-v10/date :vnok.PE.v1-v10/examination-date
                     :vnok.PE.v1-v10/is-done :vnok.PE.v1-v10/not-done-reason
                     :vnok.PE.v1-v10/has-deviations
                     :vnok.PE.v1-v10/deviations])))

(defmethod c/parse-exp-from-json 265 [inp]
  (merge
    (parser-common inp)
    {:type "vnok/PE.v2-v10"
     :date (jp/at-path
             "$.d.FormData.SectionList[0].ItemGroupList[0].RowList[0].ItemList[1].Value"
             inp)
     :examination-date
           (string/trim
             (str
               (jp/at-path
                 "$.d.FormData.SectionList[1].ItemGroupList[0].RowList[0].ItemList[1].Value"
                 inp)
               " "
               (jp/at-path
                 "$.d.FormData.SectionList[1].ItemGroupList[0].RowList[1].ItemList[1].Value"
                 inp)))
     :is-done
           (string/blank?
             (jp/at-path
               "$.d.FormData.SectionList[1].ItemGroupList[0].RowList[2].ItemList[0].Value"
               inp))
     :not-done-reason
           (jp/at-path
             "$.d.FormData.SectionList[1].ItemGroupList[0].RowList[2].ItemList[2].Value"
             inp)
     :has-deviations
           (c/yes-no-decode
             (jp/at-path
               "$.d.FormData.SectionList[2].ItemGroupList[0].RowList[0].ItemList[1].Value"
               inp))
     :deviations
           (mapv
             #(hash-map
                :organ-system (c/organ-system-decode
                                (jp/at-path "$.RowList[0].ItemList[0].Value" %))
                :is-important (c/yes-no-decode
                                (jp/at-path "$.RowList[0].ItemList[1].Value" %))
                :comment (jp/at-path "$.RowList[0].ItemList[2].Value" %))
             (next (jp/at-path "$.d.FormData.SectionList[3].ItemGroupList" inp)))
     }))

; Unplanned visit Reason (UV)
(s/def :vnok.UV/date ::c/date-time-str-or-nil)
(def uv-reason-decode {"1" "НЯ/СНЯ", "2" "Другое", "" ""})
(s/def :vnok.UV/reason (set (vals uv-reason-decode)))
(s/def :vnok.UV/comment string?)

(defmethod c/get-exp-spec "vnok/UV" [_]
  (s/merge
    ::vnok-common
    (s/keys :req-un [:vnok.UV/date :vnok.UV/reason :vnok.UV/comment])))

(defmethod c/parse-exp-from-json 263 [inp]
  (merge
    (parser-common inp)
    {:type "vnok/UV"
     :date (jp/at-path
             "$.d.FormData.SectionList[0].ItemGroupList[0].RowList[0].ItemList[1].Value"
             inp)
     :reason
           (uv-reason-decode
             (jp/at-path
               "$.d.FormData.SectionList[1].ItemGroupList[0].RowList[0].ItemList[0].Value"
               inp))
     :comment (jp/at-path
               "$.d.FormData.SectionList[1].ItemGroupList[0].RowList[3].ItemList[1].Value"
               inp)}))
