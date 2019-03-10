(ns dev
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application.
  Call `(reset)` to reload modified code and (re)start the system.
  The system under development is `system`, referred from
  `com.stuartsierra.component.repl/system`.
  See also https://github.com/stuartsierra/component.repl"
  (:require
   [clojure.core.async :as a]
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer [javadoc]]
   [clojure.pprint :refer [pprint]]
   [clojure.reflect :refer [reflect]]
   [clojure.repl :refer [apropos dir doc find-doc pst source]]
   [clojure.set :as set]
   [clojure.string :as string]
   [clojure.test :as test]
   [clojure.tools.namespace.repl :refer [refresh refresh-all clear]]
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as component]
   [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
   [clj-http.cookies :as cookies]

   [crawler.network :as network]
   [crawler.parser :as parser]
   )

  (:import (org.jsoup Jsoup)
           (org.jsoup.nodes Document Element)))

;; Do not try to load source code from 'resources' directory
(clojure.tools.namespace.repl/set-refresh-dirs "dev" "src" "test")

(defn dev-system []
  (component/system-map
    :network (network/make-network {:num-threads 10
                                    :rate 10})))

(set-init (fn [_] (dev-system)))

;(def t (parser/extract-xml-state (slurp (io/resource "parser/subject-matrix-c01.html"))))

;(def html-doc (Jsoup/parse (slurp (io/resource "psrser/subject-matrix-c01.html"))))
;(def el (.select ^Document html-doc "#__VIEWSTATE"))
;
;(.attr el "value")

;(try
;  (a/<!! (network/get (:network system) "https://www.google.com"))
;  (catch Exception e
;    (prn e (ex-data e))))
;(cookies/get-cookies (get-in system [:network :cookie-store]))
;(alter-var-root #'network/a (constantly 10))

;(log/trace "123" {:as 123 :asd 'asd})
;(network/get (:network system) "https://www.google.com")