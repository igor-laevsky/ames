(ns ui.common.meta

  (:require [clojure.string]))

;;;
;;; This namespace contains various meta information about the current project.
;;;

;;
;; Names and ordering
;;

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

;; Receives list of patients according to the db spec.
(defn sort-patients [patients]
  (sort-by :name patients))

;;
;; Exp views
;;

;; Given a list of pairs [(field-name, field-value)...] return an html table
;; representing it.
(defn- fields->table [fields]
  [:table.table-border.table
   [:tbody
    (for [p fields]
      ^{:key (first p)}
      [:tr
       [:td (first p)]
       [:td (second p)]])]])

;; Given a list of pairs [(field-name, field-kw)...] returns same list with
;; field-kw's replaced by their values from map 'm'.
(defn- get-fields [m field-names]
  (map (fn [name kw] [name (kw m)])
       (map first field-names)
       (map second field-names)))

;; Given a map and a list of pairs [(field-name, field-kw)...] returns it's
;; html representation.
(defn- map->table [m field-names]
  (-> m
      (get-fields field-names)
      (fields->table)))

(defmulti exp->view :type)

(defmethod exp->view :default [exp]
  (throw (ex-info
           (str "Unable to build view for a given exp type " (:type exp))
           {:input exp})))

(defmethod exp->view "vnok/DEMO" [exp]
  (let [field-names [["Дата визита" :date]
                     ["ДАТА ПОДПИСАНИЯ ИНФОРМИРОВАННОГО СОГЛАСИЯ" :agr-datetime]
                     ["ДАТА РОЖДЕНИЯ" :birthday]
                     ["ВОЗРАСТ" :age]
                     ["ПОЛ" :gender]
                     ["РАСА" :race]
                     ["Другое, укажите" :other]]]
    [map->table exp field-names]))

(defmethod exp->view "vnok/MD" [exp]
  [:div
   [map->table exp [[(str
                       "БЫЛИ ЛИ У ПАЦИЕНТА ОПЕРАТИВНЫЕ ВМЕШАТЕЛЬСТВА И ЗНАЧИМЫЕ, С"
                       "ТОЧКИ ЗРЕНИЯ ИССЛЕДОВАТЕЛЯ, ПРЕДШЕСТВУЮЩИЕ И СОПУТСТВУЮЩИЕ "
                       "ЗАБОЛЕВАНИЯ, ВКЛЮЧАЯ НАЛИЧИЕ В АНАМНЕЗЕ АЛЛЕРГИЧЕСКИХ РЕАКЦИЙ?")
                     :has-records]]]
   [:table
    [:thead
     [:tr
      [:th "При необходимости зарегистрируйте сведения о сопутствующей / предшествующей терапии на форме ‘ПРЕДШЕСТВУЮЩАЯ / СОПУТСТВУЮЩАЯ ТЕРАПИЯ’"]]]]
   (for [[idx rec] (map-indexed vector (:records exp))]
     ^{:key idx}
     [map->table
       rec
       [["СОСТОЯНИЕ / ДИАГНОЗ / ОПЕРАЦИЯ" :condition]
        ["ДАТА НАЧАЛА" :start-date]
        ["СТАТУС" :status]
        ["ДАТА ЗАВЕРШЕНИЯ" :stop-date]]])])

(defmethod exp->view "vnok/PE.v2-v10" [exp]
  [:div
   [map->table exp [["ДАТА ВИЗИТА" :date]
                    ["ДАТА ПРОВЕДЕНИЯ ОБСЛЕДОВАНИЯ" :examination-date]
                    ["Выполнено" :is-done]
                    ["БЫЛИ ЛИ ОБНАРУЖЕНЫ ОТКЛОНЕНИЯ ОТ НОРМЫ?" :has-deviations]]]
   [:table
    [:thead
     [:tr
      [:th "ФИЗИКАЛЬНОЕ ОБСЛЕДОВАНИЕ"]]]]
   (for [[idx rec] (map-indexed vector (:deviations exp))]
     ^{:key idx}
     [map->table
       rec
       [["Система органов" :organ-system]
        ["Клиническая значимость (в случае отклонения от нормы)" :is-important]
        ["Комментарии (в случае отклонения от нормы)" :comment]]])])

(defmethod exp->view "vnok/UV" [exp]
  (let [field-names [["ДАТА ВИЗИТА" :date]
                     ["ПРИЧИНА НЕЗАПЛАНИРОВАННОГО ВИЗИТА" :reason]
                     ["Комментарии" :comment]]]
    [map->table exp field-names]))
