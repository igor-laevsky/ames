(ns cdl.vnok
  (:require [cdl.common :as c]
            [clojure.spec.alpha :as s]
            [json-path :as jp]
            [clojure.test :as test]))

(def exp-types (->>
                 ["DEMO" "MH" "PE.v1" "PE" "UV" "VS" "AE"]
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
              :rand-num ""}
   })

; Screening visit demographic data (DEMO)
(s/def :vnok.DEMO/date ::c/date-time-str)
(s/def :vnok.DEMO/agr-datetime ::c/date-time-str)
(s/def :vnok.DEMO/birthday ::c/date-time-str)
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
           (str
             (jp/at-path
               "$.
               d.FormData.SectionList[1].ItemGroupList[0].RowList[0].ItemList[1].Value"
               inp)
             " "
             (jp/at-path
               "$.d.FormData.SectionList[1].ItemGroupList[0].RowList[1].ItemList[1].Value"
               inp))

     :birthday
           (jp/at-path
             "$.d.FormData.SectionList[2].ItemGroupList[0].RowList[0].ItemList[1].Value"
             inp)

     :age
           (Integer/parseInt
             (jp/at-path
               "$.d.FormData.SectionList[2].ItemGroupList[0].RowList[1].ItemList[1].Value"
               inp))

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
