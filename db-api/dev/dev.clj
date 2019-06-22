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
    [com.stuartsierra.component :as component]
    [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
    [io.pedestal.http :as http]
    [io.pedestal.test :refer [response-for]]
    [io.pedestal.http.route :as route]
    [qbits.spandex :as es]
    [orchestra.spec.test :as st]

    [cdl.core :as cdl]

    [db-api.main :as main]
    [db-api.handlers :as handlers]
    [db-api.pedestal :as p]))

(st/instrument)

(clojure.tools.namespace.repl/set-refresh-dirs "dev" "src" "test" "checkouts")

(def dev-service-map
  (-> main/common-service-map
      (merge {:env         :dev
              ::http/join? false})
      (http/default-interceptors)
      (http/dev-interceptors)))

(defn dev-system []
  (component/system-map
    :pedestal (p/make-pedestal dev-service-map)))

(set-init (fn [_] (dev-system)))

;(def s (es/client {:hosts ["http://127.0.0.1:9200"]}))
;(def resp (es/request s
;            {:method :get
;             :url "vnok/_search?q=01-002&size=1000&from=10"}))
;(count (get-in resp [:body :hits :hits]))