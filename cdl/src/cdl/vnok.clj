(ns cdl.vnok
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as ss]
            [json-path :as jp]

            [cdl.utils :as utils]
            [cdl.lang :refer :all]))

;;
;; Common field types
;;

(def visit-names ["se.SCR" "se.SCR1" "se.V1" "se.V2" "se.V3" "se.V4" "se.V5"
                  "se.V6" "se.V7" "se.V8" "se.V9" "se.V10" "se.V11" "se.V12"
                  "se.UV" "se.CM" "se.PR" "se.AE" "se.DV"])

(deftype LocationFieldType [loc]
  Field
  (from-json [_ inp]
    (some-> loc (jp/at-path inp) (utils/is-str) (ss/trim) (not-empty) (subs 1)))
  (get-spec-form [_ kw]
    [`(s/def ~kw (s/and string? (complement empty?)))])
  (get-es-mapping [_] {:type "keyword"}))

(deftype StatusFieldType [loc true-vals]
  Field
  (from-json [_ inp]
    (let [res (jp/at-path loc inp)]
      (when res
        (-> res (true-vals) (some?)))))
  (get-spec-form [_ kw] [`(s/def ~kw boolean?)])
  (get-es-mapping [_] {:type "boolean"}))

(defn yes-no-field [loc] (->Enumeration loc utils/yes-no-decode))
(defn gender-field [loc] (->Enumeration loc utils/gender-decode))

;;
;; Study description
;;

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
                             :gender (gender-field "$.d.SubjectSex")
                             :rand-num (->Str "$.context.rand-num")})})

(def exps
  [{:name "vnok.DEMO"
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
         :gender (gender-field "$.d.FormData.SectionList[2].ItemGroupList[0].RowList[2].ItemList[1].Value")
         :race (->Enumeration
                 "$.d.FormData.SectionList[2].ItemGroupList[0].RowList[4].ItemList[1].Value"
                 utils/race-decode)
         :other (->Text "$.d.FormData.SectionList[2].ItemGroupList[0].RowList[5].ItemList[1].Value")
         }))}

   {:name "vnok.MD"
    :raw-json-id 205
    :fields
    (map->Composite
      (merge
        common-fields
        {:type (->ConstantVal "vnok.MD")
         :has-records (yes-no-field "$.d.FormData.SectionList[0].ItemGroupList[0].RowList[0].ItemList[1].Value")
         :records
         (->Array
           "$.d.FormData.SectionList[2].ItemGroupList"
           (map->Composite
             {:condition (->Text "$.RowList[0].ItemList[1].Value")
              :status (->Enumeration "$.RowList[2].ItemList[1].Value"
                                     utils/status-decode)
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
         :has-deviations (yes-no-field "$.d.FormData.SectionList[2].ItemGroupList[0].RowList[0].ItemList[1].Value")
         :deviations
         (->Array
           "$.d.FormData.SectionList[3].ItemGroupList"
           (map->Composite
             {:organ-system (->Enumeration
                              "$.RowList[0].ItemList[0].Value"
                              utils/organ-system-decode)
              :is-important (yes-no-field "$.RowList[0].ItemList[1].Value")
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

;;
;; Actions avaliable for this study
;;

(defn dispatch-parser [json] (get-in json [:d :FormData :SectionList 0 :ID]))
(def-json-parser parse-exp-from-json dispatch-parser exps)

(def-exp-specs ::exp exps)
