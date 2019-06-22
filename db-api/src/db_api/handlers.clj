(ns db-api.handlers
  (:require [io.pedestal.interceptor.helpers :as helpers]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [ring.util.response :as ring-resp]
            [qbits.spandex :as spandex]

            [db-api.pedestal :as c]))

;; Just a simple full text search over the whole database
;;
(defn search-handler [req]
  (let [es (c/use-component req :es)]
    (ring-resp/response
      (->
        (spandex/request
          (:es-client es)
          {:method :get
           :url    "vnok/_search?q=01-002&size=1000&from=10"})
        :body))))

(def common-interceptors [http/json-body])

(def routes
  (route/expand-routes
    #{["/greet"
       :get (fn [req] (ring-resp/response "Hello world!")) :route-name :greet]
      ["/search"
       :get (conj common-interceptors (c/using-component :es) search-handler)
       :route-name :search]}))
