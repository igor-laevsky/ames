(ns crawler.etl
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [clj-time.format :as time-fmt]
            [clj-time.core :as time]
            [clojure.core.async :as a]

            [crawler.network :as net]
            [crawler.saver :as s]))

;;; Extracts all exps in a given site and saves them using 'saver'.
;;;

(defrecord ETL [network saver params]
  component/Lifecycle
  (start [this]
    (log/info "Starting ETL")
    this)
  (stop [this]
    (log/info "Stopping ETL")
    this))

;; Creates non-started etl. 'params' should contain:
;;   :login, :password, :main-url
(defn make-etl [params] (map->ETL {:params params}))

(defn- login-url [{{:keys [main-url]} :params}]
  (str main-url "MainService.asmx/LoginUser"))
(defn- subject-matrix-url [{{:keys [main-url]} :params}]
  (str main-url "SubjectMatrix.aspx"))

;; Login into application. Blocks main thread while waiting for the response.
;; Return true on success false on failure.
(defn login! [{{:keys [login password]} :params :as etl}]
  (let [login-data-format
        "{login:\"%s\", password:\"%s\", localTime:\"%s UTC +0300\",
         userAgent:\"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36\"}"
        cur-date-str (time-fmt/unparse
                       (time-fmt/formatter "MM/dd/yyyy HH:mm:ss")
                       (time/from-time-zone (time/now)
                                            (time/time-zone-for-offset -3)))
        login-data (format login-data-format login password cur-date-str)
        try-login
        (fn [] (a/<!! (net/post (:network etl)
                                (login-url etl)
                                {:content-type :json
                                 :body login-data})))
        parse-response
        (fn [resp] (cond
                     (not= (:status resp) 200) :error
                     (.contains (:body resp) "loghasusererror") :has-user
                     (.contains (:body resp) "logok") :ok))]
    (case (parse-response (try-login))
      :error false
      :ok true
      :has-user (= (parse-response (try-login)) :ok))))

(defn subject-matrix [{:keys [network] :as etl}]
  (net/get network (subject-matrix-url etl)))
