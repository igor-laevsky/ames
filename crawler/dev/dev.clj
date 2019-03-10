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
   [clojure.data.json :as js]
   [clojure.spec.alpha :as s]
   [com.stuartsierra.component :as component]
   [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
   [clj-http.cookies :as cookies]
   [orchestra.spec.test :as st]
   [clj-time.format :as tf]
   [clj-time.core :as t]
   [clj-time.local :as l]

   [crawler.network :as network]
   [crawler.extractor :as extractor]
   [crawler.saver :as saver]
   [crawler.etl :as etl]
   [cdl.core :as cdl])

  (:import (org.jsoup Jsoup)
           (org.jsoup.nodes Document Element)))

(st/instrument)

;; Do not try to load source code from 'resources' directory
(clojure.tools.namespace.repl/set-refresh-dirs "dev" "src" "test" "checkouts")

(def creds (read-string (slurp (io/reader (io/resource "creds.edn")))))

(defn dev-system []
  (component/system-map
    :network (network/make-network {:num-threads 10
                                    :rate 10})
    :saver (saver/make-file-saver {:file-name "test/resources/saver/tmp.json"})
    :etl (component/using
           (etl/make-etl creds)
           [:network :saver])))

(set-init (fn [_] (dev-system)))

