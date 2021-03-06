(ns dev
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application.
  Call `(reset)` to reload modified code and (re)start the system.
  The system under development is `system`, referred from
  `com.stuartsierra.component.repl/system`.
  See also https://github.com/stuartsierra/component.repl"
  (:require
    [clojure.pprint :as pp]
    [clojure.tools.namespace.repl :refer [refresh]]
    [clojure.data.json :as json]
    [com.stuartsierra.component :as component]
    [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
    [io.pedestal.http :as http]
    [io.pedestal.test :refer [response-for]]
    [io.pedestal.http.route :as route]
    [qbits.spandex :as spandex]
    [orchestra.spec.test :as st]
    [ring.util.codec :as c]

    [cdl.core :as cdl]

    [db-api.core :as core]
    [db-api.es :as es]
    [db-api.handlers :as handlers]
    [db-api.pedestal :as p]))

(st/instrument)

(clojure.tools.namespace.repl/set-refresh-dirs "dev" "src" "test" "checkouts")

(def dev-service-map
  (-> core/common-service-map
      (merge {:env         :dev
              ::http/join? false})
      (http/default-interceptors)
      (http/dev-interceptors)))

(defn dev-system []
  (component/system-map
    :es (es/make-es {:host "http://127.0.0.1:9200" :index "vnok"})
    :pedestal (component/using
                (p/make-pedestal dev-service-map)
                [:es])))

(set-init (fn [_] (dev-system)))

(def url-for (route/url-for-routes handlers/routes))

(defn get-service-fn [system]
  (get-in system [:pedestal :server ::http/service-fn]))
