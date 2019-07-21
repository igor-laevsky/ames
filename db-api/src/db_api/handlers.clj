(ns db-api.handlers
  (:require [clojure.tools.logging :as log]
            [io.pedestal.interceptor.helpers :as helpers]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [ring.util.response :as ring-resp]
            [ring.util.codec :as ring-codec]
            [qbits.spandex :as spandex]

            [db-api.pedestal :as c]
            [db-api.es :as es]))

;; Just a simple full text search over the whole database.
;; Returns results in the form of
;; {:total "11",
;;  :hits [{:id "123" :_source {...fields...}}, ...]}
;;
(defn search-handler [req]
  (let [es (c/use-component req :es)
        query (some-> (get-in req [:query-params :q])
                      (clojure.string/escape {\/ "\\/"})
                      (ring-codec/url-encode))
        from (get-in req [:query-params :from] "0")
        size (get-in req [:query-params :size] "1000")]
    (if query
      (-> (es/search es query from size)
          :hits
          (ring-resp/response))
      (throw (ex-info "Expected query string as an argument" {:request req})))))

;; Helper method for the get-locations and get-patients.
;; Receives aggregation results from elasticsearch and outputs them in a format
;; expected by the api caller.
;;
(defn- es-to-list [inp key]
  (->> inp
       :aggregations key :buckets
       (map #(array-map :name (:key %)
                        :total (:doc_count %)
                        :verified (->> %
                                       :verified :buckets
                                       (filter (fn [e] (= (:key e) 1)))
                                       (first)
                                       :doc_count
                                       ((fnil int 0)))))))

;; List all avaliable locations.
;; Returns response in the form of:
;;   [{:name "13", :total <int>, :verified <int>}, ...]
;;
(defn get-locations [req]
  (-> (es/list-locations (c/use-component req :es))
      (es-to-list :locations)
      (ring-resp/response)))

;; Helper function. Expands patient/visit names into name-kw and exp-kw.
;; I.e transforms {:name "<name> <rand-num" ...} into
;; {:name "<name>" :rand-num "<rand-num>" ...}
;;
(defn- expand-names [inp name-kw exp-kw]
  (map #(let [s (-> (name-kw %) (clojure.string/split #" "))
              [name rand-num] (filter (complement empty?) s)]
          (if rand-num
            (merge % {name-kw name exp-kw rand-num})
            (merge % {name-kw name}))) inp))

;; List patients for a given locations.
;; Expects "?loc=..." in the query-params.
;; Returns results in the form of
;;   [{:name "01-002", :rand-num "R001", :total <int>, :verified <int>}, ...]
;;
(defn get-patients [req]
  (if-let [loc (get-in req [:query-params :loc])]
    (-> (es/list-patients (c/use-component req :es) loc)
        (es-to-list :patients)
        (expand-names :name :rand-num)
        (ring-resp/response))
    (throw (ex-info "Expected location as an argument" req))))

;; Lists visits for a given patient in a given location.
;; Expects "?loc=...&pat=..." in the query-params.
;; Returns results in the form of:
;;   [{:name "se.SCR", :verified <int>, :total <int>}, ...]
;;
(defn get-visits [req]
  (let [loc (get-in req [:query-params :loc])
        patient (get-in req [:query-params :pat])]
    (if (and loc patient)
      (-> (es/list-visits (c/use-component req :es) loc patient)
          (es-to-list :visits)
          (expand-names :name :group)
          (ring-resp/response))
      (throw (ex-info "Expected location as an argument" req)))))

;; List exps for a given patient in a given center sorted by date.
;; Expects patient name in the query-params. From and size arguments are optional.
;; Returns list of exps.
;;
(defn get-exps [req]
  (let [es (c/use-component req :es)
        name (get-in req [:query-params :name])
        from (get-in req [:query-params :from] 0)
        size (get-in req [:query-params :size] 1000)]
    (if name
      (->> (es/list-exps es name from size)
           :hits :hits ; really, nested in a two equal keys
           (map :_source)
           (ring-resp/response))
      (throw (ex-info "Expected name as an argument" req)))))

(def common-interceptors [http/json-body])

(def routes
  (route/expand-routes
    #{["/search"
       :get (conj common-interceptors (c/using-component :es) search-handler)
       :route-name :search]
      ["/locations"
       :get (conj common-interceptors (c/using-component :es) get-locations)
       :route-name :locations]
      ["/patients"
       :get (conj common-interceptors (c/using-component :es) get-patients)
       :route-name :patients]
      ["/visits"
       :get (conj common-interceptors (c/using-component :es) get-visits)
       :route-name :visits]
      ["/exps"
       :get (conj common-interceptors (c/using-component :es) get-exps)
       :route-name :exps]}))
