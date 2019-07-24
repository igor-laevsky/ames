(ns ui.common.meta)

;;;
;;; This namespace contains various meta information about the current project.
;;;

;; Human readable visit names (use below functions)
(def ^:private visit-names
  {"SCR" "Скрининг / День (-14 -1)"
   "SCR1" "Скрининг / День (-7….-1)"
   "V1" "Визит 1 / День 1"
   "V2" "Визит 2 / День 2"
   "V3" "Визит 3 / День 3"
   "V4" "Визит 4 / День 4"
   "V5" "Визит 5 / День 5"
   "V6" "Визит 6 / День 6"
   "V7" "Визит 7 / День 7"
   "V8" "Визит 8 / День 8"
   "V9" "Визит 9 / День 9"
   "V10" "Визит 10 / День 10"
   "V11" "Визит 11 / День 11"
   "V12" "Визит 12 / День 12"
   "UV" "Незапланированный визит"
   "EOS" "Завершение исследования"
   "CM" "Предшествующая сопутствующая терапия"
   "PR" "Сопутствующие процедуры"
   "AE" "Нежелательное явление"
   "DV" "Нарушения отклонения от протокола"})

(defn- remove-visit-prefix [visit-name]
  (when visit-name
    (clojure.string/replace visit-name "vnok/se." "")))

(defn visit->name [{:keys [name group]}]
  (let [clean-name (some-> name
                           remove-visit-prefix
                           visit-names)]
    (if group
      (str clean-name " (" group ")")
      clean-name)))

(defn visit->url-name [{:keys [name group]}]
  (let [clean-name (remove-visit-prefix name)]
    (if group
      (str clean-name "-" group)
      clean-name)))

(defn url-name->visit [inp]
  (let [[name group] (clojure.string/split inp #"-")
        full-name (str "vnok/se." name)]
    (if group
      {:name full-name :group group}
      {:name full-name})))

(def ^:private visit-order
  ["SCR" "SCR1" "V1" "V2" "V3" "V4" "V5" "V6" "V7" "V8" "V9" "V10" "V11" "V12"
   "UV" "EOS" "CM" "PR" "AE" "DV"])

;; Receives list of visits according to the db spec.
(defn sort-visits [visits]
  (let [order (zipmap visit-order (range))]
    (sort-by
      #(vector
         (-> % :name remove-visit-prefix order)
         (:group %))
      visits)))
