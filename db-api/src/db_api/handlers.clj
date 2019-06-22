(ns db-api.handlers
  (:require [io.pedestal.interceptor.helpers :as helpers]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [ring.util.response :as ring-resp]
            [qbits.spandex :as spandex]))

;; Elasticsearch interceptor. Embeds a spandex connection into the context.
;;
(def ^:const es-host "http://127.0.0.1:9200")
(def ^:const es-index "vnok")
(def es-interceptor
  {:name :es-interceptor
   :enter (fn [ctx]
            (assoc ctx :es-client (spandex/client {:hosts [es-host]})))})

(def common-interceptors [http/json-body])

;; Just a simple full text search over the whole database
;;
(def search-handler
  {:name :search
   :enter
   (fn [ctx]
     (let [es (:es-client ctx)]
       [1 2 3])
     )})

(def routes
  (route/expand-routes
    #{["/greet"
       :get (fn [req] (ring-resp/response "Hello world!")) :route-name :greet]
      ["/search"
       :get (conj common-interceptors es-interceptor search-handler)]}))
