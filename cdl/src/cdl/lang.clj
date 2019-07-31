(ns cdl.lang
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as ss]
            [json-path :as jp]
            [clojure.data.json :as js]
            [clojure.java.io :as io]

            [cdl.common :as common :refer [is-str]]))

(defprotocol Field
  "Basic functionality implemented by the each field type."
  (from-json [this inp]
    "Parse this field from the raw json. Returns field value.
     Returns nil when unable to parse.")
  (get-spec-form [this]
    "Returns the spec form for this field or a map from the spec name
     to it's form in case if (s/keys) mapping is neccessary.")
  (get-es-mapping [this] "Returns ES mapping for this field."))

(deftype ConstantVal [value]
  Field
  (from-json [_ inp] value)
  (get-spec-form [_] `#{~value})
  (get-es-mapping [_] {:type "keyword"}))

(deftype Bool [loc]
  Field
  (from-json [_ inp] (some-> loc (jp/at-path inp) (str) (ss/trim) (ss/blank?)))
  (get-spec-form [_] `boolean?)
  (get-es-mapping [_] {:type "boolean"}))

(deftype Str [loc]
  Field
  (from-json [_ inp] (some-> loc (jp/at-path inp) (is-str) (ss/trim) (not-empty)))
  (get-spec-form [_] `(s/and string? (complement empty?)))
  (get-es-mapping [_] {:type "keyword"}))

(deftype Text [loc]
  Field
  (from-json [_ inp] (some-> loc (jp/at-path inp) (is-str) (ss/trim) (not-empty)))
  (get-spec-form [_] `(s/and string? (complement empty?)))
  (get-es-mapping [_] {:type "text"}))

(deftype Enumeration [loc key->val]
  Field
  (from-json [_ inp] (some-> loc (jp/at-path inp) key->val))
  (get-spec-form [_] `#{~@(set (vals key->val))})
  (get-es-mapping [_] {:type "text"}))

(deftype Date [loc]
  Field
  (from-json [_ inp] (some-> loc (jp/at-path inp) (is-str) (ss/trim) (not-empty)))
  (get-spec-form [_] `common/date-time-str?)
  (get-es-mapping [_] {:type "date"
                       :format "yyyy-MM-dd HH:mm|yyyy-MM-dd|yyyy-MM|yyyy"}))

(deftype DateTime [date-loc time-loc]
  Field
  (from-json [_ inp]
    (-> (str (some-> date-loc (jp/at-path inp) (is-str) (ss/trim))
             " "
             (some-> time-loc (jp/at-path inp) (is-str) (ss/trim)))
        (ss/trim)
        (not-empty)))
  (get-spec-form [_] `common/date-time-str?)
  (get-es-mapping [_] {:type "date"
                       :format "yyyy-MM-dd HH:mm|yyyy-MM-dd|yyyy-MM|yyyy"}))

(deftype Array [loc element-type skip-first?]
  Field
  (from-json [_ inp]
    "Expects to find something sequential as a raw json input.
     Removes unparsed values."
    (let [arr (jp/at-path loc inp)]
      (when (sequential? arr)
        (->> (if skip-first? (next arr) arr)
             (mapv #(from-json element-type %))
             (filter (complement nil?))
             (not-empty)))))
  (get-spec-form [_] `(s/and
                        (s/coll-of ~(get-spec-form element-type))
                        (complement empty?)))
  ; Every field in ES is an array anyway, so no transformation is needed.
  (get-es-mapping [_] (get-es-mapping element-type)))

;; Removes nil keys from the map.
(defn- remove-empty-keys [m]
  (into (empty m) (remove (comp nil? val) m)))

;; These intentionally drop the data type of the input map. This is neccessary
;; to simplify their behaviour on records.
(defn- map-vals [m f] (into {} (map (fn [[k v]] [k (f v)]) m)))
(defn- map-keys [m f] (into {} (map (fn [[k v]] [(f k) v]) m)))

(defrecord Composite []
  Field
  (from-json [this inp]
    (-> (map-vals this #(from-json % inp))
        (remove-empty-keys)
        (not-empty)))
  (get-spec-form [this]
    (map-vals this get-spec-form))
  (get-es-mapping [this]
    {:properties (map-vals this get-es-mapping)}))

;; Transforms result of the 'get-spec-form' into a list of spec defs.
(defn to-spec-defs [kw spec-form]
  (assert (and (keyword? kw) (namespace kw)) "kw must be a namespaced keyword")
  (if (map? spec-form)
    (let [named-specs (map-keys spec-form
                                #(keyword (str (namespace kw) "." (name kw))
                                          (name %)))]
      (conj
        (vec (mapcat (fn [[k v]] (to-spec-defs k v)) named-specs))
        `(s/def ~kw (s/keys :opt-un [~@(keys named-specs)]))))
    [`(s/def ~kw ~spec-form)]))

;; Helper for the def-json-parser.
;; Returns form which defines multimethod case for a given exp.
(defn- get-exp-parser [parser-name exp]
  `(defmethod ~parser-name ~(:raw-json-id exp) [~'inp]
     (from-json ~(:fields exp) ~'inp)))

;; Generate json parser from a list of exp descriptions.
(defmacro def-json-parser [parser-name f]
  (let [exps (eval f)]
    `(do
       (defmulti ~parser-name common/dispatch-parser)
       (defmethod ~parser-name :default [~'j]
         (throw (ex-info
                  (str "Unable to find parser for a given exp.")
                  {:orig-id (get-in ~'j [:d :FormData :SectionList 0 :ID])})))
       ~@(for [e exps] (get-exp-parser parser-name e)))))

;; Defines all specs required for a set of exps.
(defmacro def-exp-specs [root-name f]
  (assert (string? root-name) "root-name must be a string")
  (let [exps (eval f)
        m-name (symbol (str *ns*) (str "get-" root-name "-spec"))
        get-spec-name (fn [e] (keyword root-name (name (:name e))))]
    `(do
       ; s/def all of the specs
       ~@(for [e exps
               :let [spec-name (get-spec-name e)]]
           (->> e :fields (get-spec-form) (to-spec-defs spec-name)))

       ; define multimethod and a root spec
       (defmulti ~m-name :type)
       ~@(for [e exps]
           `(defmethod ~m-name ~(:name e) [~'_]
              (s/get-spec ~(get-spec-name e)))))))
