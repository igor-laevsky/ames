(ns cdl.vnok
  (:require [cdl.common :as c]
            [clojure.spec.alpha :as s]))

(def exp-types (->>
                 ["DEMO" "MH" "PE.v1" "PE" "UV" "VS" "AE"]
                 (map (partial str "vnok/"))
                 (set)))

(def visit-types (->>
                   ["SCR" "SCR1" "V1" "V2" "V3" "V4" "V5" "V6"
                    "V7" "V8" "V9" "V10" "V11" "V12" "UV" "CM"
                    "PR" "AE" "DV"]
                   (map (partial str "vnok/"))
                   (set)))

; Refined common spec to be used in all of the vnok experiments.
(s/def ::type exp-types)
(s/def ::visit visit-types)
(s/def ::location (set (map (partial format "%02d") (range 1 19))))
(s/def ::vnok-common
  (s/merge ::c/exp-common
           (s/keys :req-un [::type ::visit ::location])))

; Screening visit demographic data
(def DEMO
  {:name "vnok/DEMO"
   :orig-id 264})
(defmethod c/get-exp-spec "vnok/DEMO" [_]
  (s/get-spec ::vnok-common))
(defmethod c/parse-exp-from-json 264 [_]
  {:type "vnok/DEMO"})

; Define visits
(def visits
  {::SCR {:long-name "Скрининг / День (-14 -1)"
         :req-exps []}
   ::SCR1 {:long-name "Скрининг / День (-7….-1)"
          :req-exps []}
   ::V1 {:long-name "Период терапии / Визит 1 / День 1"
         :req-exps []}
   ::V2 {:long-name "Период терапии / Визит 2 / День 2"
         :req-exps []}
   ::V6 {:long-name "Период терапии / Визит 6 / День 6"
         :req-exps []}
   ::V12 {:long-name "Период наблюдения / Визит 12 / День (40 +/-2 )"
          :req-exps []}
   ::EOS {:long-name "Завершение исследования"
          :req-exps []}

   ::UV {:long-name "Незапланированный визит"}
   ::AE {:long-name "Нежелательное явление"}
   })
