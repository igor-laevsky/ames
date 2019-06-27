(ns db-api.handlers
  (:require [clojure.tools.logging :as log]
            [io.pedestal.interceptor.helpers :as helpers]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [ring.util.response :as ring-resp]
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
        query (get-in req [:query-params :q])
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

;; List patients for a given locations.
;; Expects "&loc=..." in the query-params.
;; Returns results in the form of
;;   [{:name "01-002 <randnum>", :total <int>, :verified <int>}, ...]
;;
(defn get-patients [req]
  (if-let [loc (get-in req [:query-params :loc])]
    (-> (es/list-patients (c/use-component req :es) loc)
        (es-to-list :patients)
        (ring-resp/response))
    (throw (ex-info "Expected location as an argument" req))))

;; List exps for a given patient in a given center sorted by date.
(defn get-exps [req]
  (ring-resp/response "TODO"))

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
       :route-name :patients]}))
