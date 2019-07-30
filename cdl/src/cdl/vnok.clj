(ns cdl.vnok
  (:require [clojure.spec.alpha :as s]
            [json-path :as jp]
            [clojure.test :as test]
            [clojure.string :as ss]

            [cdl.common :as c]
            [cdl.lang :refer :all]))

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
(s/def ::location (set (map (partial format "%02d") (range 1 33))))
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

; Unplanned visit Reason (UV)
(s/def :vnok.UV/date ::c/date-time-str-or-nil)
(def uv-reason-decode {"1" "НЯ/СНЯ", "2" "Другое", "" ""})
(s/def :vnok.UV/reason (set (vals uv-reason-decode)))
(s/def :vnok.UV/comment string?)

(defmethod c/get-exp-spec "vnok/UV" [_]
  (s/merge
    ::vnok-common
    (s/keys :req-un [:vnok.UV/date :vnok.UV/reason :vnok.UV/comment])))

;;
;; New way for defining exps
;;

(def visit-names ["se.SCR" "se.SCR1" "se.V1" "se.V2" "se.V3" "se.V4" "se.V5"
                  "se.V6" "se.V7" "se.V8" "se.V9" "se.V10" "se.V11" "se.V12"
                  "se.UV" "se.CM" "se.PR" "se.AE" "se.DV"])


(deftype LocationFieldType [loc]
  Field
  (from-json [_ inp]
    (some-> loc (jp/at-path inp) (c/is-str) (ss/trim) (not-empty) (subs 1)))
  (get-spec-form [_] `(s/and string? (complement empty?)))
  (get-es-mapping [_] {:type "keyword"}))

(deftype StatusFieldType [loc true-vals]
  Field
  (from-json [_ inp]
    (let [res (jp/at-path loc inp)]
      (when res
        (-> res (true-vals) (some?)))))
  (get-spec-form [_] `boolean?)
  (get-es-mapping [_] {:type "boolean"}))

(def common-fields
  {:visit (->Enumeration "$.d.StudyEventOID"
                         (zipmap visit-names
                                 (map (partial str "vnok/") visit-names)))
   :location (->LocationFieldType "$.d.LocationOID")
   :group (->Str "$.d.StudyEventRepeatKey")
   :finished (->StatusFieldType "$.d.State" #{6 8})
   :verified (->StatusFieldType "$.d.State" #{7 8})
   :patient (map->Composite {:name (->Str "$.d.SubjectKey")
                             :birthday (->Date "$.d.SubjectBrthDate")
                             :gender (->Enumeration "$.d.SubjectSex"
                                                    c/gender-decode)
                             :rand-num (->Str "$.context.rand-num")})})

(def exps
  [
   {:name "vnok.DEMO"
    :raw-json-id 264
    :fields
    (map->Composite
      (merge
        common-fields
        {:type (->ConstantVal "vnok.DEMO")
         :date (->Date "$.d.FormData.SectionList[0].ItemGroupList[0].RowList[0].ItemList[1].Value")
         :agr-datetime (->DateTime
                         "$.d.FormData.SectionList[1].ItemGroupList[0].RowList[0].ItemList[1].Value"
                         "$.d.FormData.SectionList[1].ItemGroupList[0].RowList[1].ItemList[1].Value")
         :birthday (->Date "$.d.FormData.SectionList[2].ItemGroupList[0].RowList[0].ItemList[1].Value")
         :age (->Str "$.d.FormData.SectionList[2].ItemGroupList[0].RowList[1].ItemList[1].Value")
         :gender (->Enumeration
                   "$.d.FormData.SectionList[2].ItemGroupList[0].RowList[2].ItemList[1].Value"
                   c/gender-decode)
         :race (->Enumeration
                 "$.d.FormData.SectionList[2].ItemGroupList[0].RowList[4].ItemList[1].Value"
                 c/race-decode)
         :other (->Text "$.d.FormData.SectionList[2].ItemGroupList[0].RowList[5].ItemList[1].Value")
         }))}

   {:name "vnok.MD"
    :raw-json-id 205
    :fields
    (map->Composite
      (merge
        common-fields
        {:type (->ConstantVal "vnok.MD")
         :has-records (->Enumeration "$.d.FormData.SectionList[0].ItemGroupList[0].RowList[0].ItemList[1].Value"
                                     c/yes-no-decode)
         :records
         (->Array
           "$.d.FormData.SectionList[2].ItemGroupList"
           (map->Composite
             {:condition (->Text "$.RowList[0].ItemList[1].Value")
              :status (->Enumeration "$.RowList[2].ItemList[1].Value"
                                     c/status-decode)
              :start-date (->Date "$.RowList[1].ItemList[1].Value")
              :stop-date (->Date "$.RowList[4].ItemList[1].Value")})
           false)}
        ))}

   {:name "vnok.PE.v2-v10"
    :raw-json-id 265
    :fields
    (map->Composite
      (merge
        common-fields
        {:type (->ConstantVal "vnok.PE.v2-v10")
         :date (->Date "$.d.FormData.SectionList[0].ItemGroupList[0].RowList[0].ItemList[1].Value")
         :examination-date (->DateTime "$.d.FormData.SectionList[1].ItemGroupList[0].RowList[0].ItemList[1].Value"
                                       "$.d.FormData.SectionList[1].ItemGroupList[0].RowList[1].ItemList[1].Value")
         :is-done (->Bool "$.d.FormData.SectionList[1].ItemGroupList[0].RowList[2].ItemList[0].Value")
         :not-done-reason (->Text "$.d.FormData.SectionList[1].ItemGroupList[0].RowList[2].ItemList[2].Value")
         :has-deviations (->Enumeration
                           "$.d.FormData.SectionList[2].ItemGroupList[0].RowList[0].ItemList[1].Value"
                           c/yes-no-decode)
         :deviations
         (->Array
           "$.d.FormData.SectionList[3].ItemGroupList"
           (map->Composite
             {:organ-system (->Enumeration
                              "$.RowList[0].ItemList[0].Value"
                              c/organ-system-decode)
              :is-important (->Enumeration
                              "$.RowList[0].ItemList[1].Value"
                              c/yes-no-decode)
              :comment (->Text "$.RowList[0].ItemList[2].Value")})
           true)}))}

   {:name "vnok.UV"
    :raw-json-id 263
    :fields
    (map->Composite
      (merge
        common-fields
        {:type (->ConstantVal "vnok.UV")
         :date (->Date "$.d.FormData.SectionList[0].ItemGroupList[0].RowList[0].ItemList[1].Value")
         :reason (->Enumeration
                   "$.d.FormData.SectionList[1].ItemGroupList[0].RowList[0].ItemList[0].Value"
                   {"1" "НЯ/СНЯ", "2" "Другое", "" ""})
         :comment (->Text "$.d.FormData.SectionList[1].ItemGroupList[0].RowList[3].ItemList[1].Value")}))}])

(def-json-parser parse-exp-from-json exps)
