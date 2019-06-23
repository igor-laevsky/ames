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

(def common-interceptors [http/json-body])

(def routes
  (route/expand-routes
    #{["/search"
       :get (conj common-interceptors (c/using-component :es) search-handler)
       :route-name :search]}))
