(ns db-api.handlers
  (:require [io.pedestal.interceptor.helpers :as helpers]
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
      (ring-resp/response (:hits (es/search es query from size)))
      (throw (ex-info "Expected query string as an argument" {:request req})))))

;; List all avaliable locations.
;; Returns response in the form of:
;;   [{:name "13", :total <int>, :verified <int>}, ...]
;;
(defn get-locations [req]
  (ring-resp/response
    (->> (es/list-locations (c/use-component req :es))
         :aggregations :locations :buckets
         (map #(array-map :name (:key %)
                          :total (:doc_count %)
                          :verified (->> %
                                         :verified :buckets
                                         (filter (fn [e] (= (:key e) 1)))
                                         (first)
                                         :doc_count
                                         (fnil int 0)))))))

;; List patients for a given locations.
;; Expects "&loc=..." in the query-params.
;; Returns results in the form of
;;   [{:name "01-002 <randnum>", :total <int>, :verified <int>}, ...]
;;
(defn get-patients [req]
  (ring-resp/response "TODO"))

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
       :route-name :locations]}))
