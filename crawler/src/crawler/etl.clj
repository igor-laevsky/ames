(ns crawler.etl
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [clj-time.format :as time-fmt]
            [clj-time.core :as time]
            [clojure.core.async :as a]

            [crawler.network :as net]
            [crawler.extractor :as extr]
            [crawler.saver :as s]))

;;; Extracts all exps in a given site and saves them using 'saver'.
;;;

;; Commonly used URL's here. Accept ETL as their first parameter.
(defn- login-url [{{:keys [main-url]} :params}]
  (str main-url "MainService.asmx/LoginUser"))
(defn- subject-matrix-url [{{:keys [main-url]} :params}]
  (str main-url "SubjectMatrix.aspx"))
(defn- keep-session-url [{{:keys [main-url]} :params}]
  (str main-url "keepsession.aspx?rnd=" (rand)))

;; Get request for the subject matrix. Returns promise chanel.
(defn subject-matrix [{:keys [network] :as etl}]
  (net/get network (subject-matrix-url etl)))

;; Helper function to perform RCP call .net style.
;; Blocks caller while waiting for the response. Returns non-modified server
;; response map as per clj-http. There is no way to check if server had received
;; the callback so this will silently not work in case of any errors.
(defn- call-server-menu! [etl name]
  (let [view-state (some-> (subject-matrix etl)
                           (a/<!!)
                           (:body)
                           (extr/extract-view-state))
        args (merge view-state {"__CALLBACKID" "ctl00"
                                "__CALLBACKPARAM" name})]
    (-> (net/post (:network etl)
                  (subject-matrix-url etl)
                  {:form-params args})
        (a/<!!))))

;; Login into application. Blocks caller thread while waiting for the response.
;; Returns true on success nil on failure.
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
                     (.contains (:body resp) "logok") :ok))

        throw-err (fn [] (throw (ex-info "Failed to login into the system" {})))]
    (case (-> (try-login) (parse-response))
      :error (throw-err)
      :ok true
      :has-user (case (-> (try-login) (parse-response))
                  :error (throw-err)
                  :ok true
                  :has-user (throw-err)))))

;; Requests server to continue current user session. Doesn't block.
;; Returns promise chanel with the network response.
(defn keep-session [etl]
  (net/get (:network etl) (keep-session-url etl)))

(defn start-keep-session-service [etl]
  (let [close-chan (a/chan)]
    (a/go-loop []
      (let [[_ res] (a/alts! [close-chan (a/timeout 500000)])]
        (when-not (= res close-chan)
          (log/info "Sending keep session request")
          (a/<! (keep-session etl))
          (recur))))
    close-chan))

(defn stop-keep-session-service [service]
  (a/close! service))

;; Logout current user. Blocks caller thread.
(defn logout! [etl]
  (call-server-menu! etl "logout"))


;; ETL which doesn't login, logout or start keep session service.
(defrecord PlainETL [network saver params]
  component/Lifecycle
  (start [this]
    (log/info "Starting plain ETL")
    this)
  (stop [this]
    (log/info "Stopping plain ETL")
    this))

;; Creates non-started etl. 'params' should contain:
;;   :login, :password, :main-url
(defn make-plain-etl [params] (map->PlainETL {:params params}))

;; ETL which logins user at start, logouts at stop and keeps it's session
;; alive using dedicated go block.
(defrecord LoginETL [network saver keep-session params]
  component/Lifecycle
  (start [this]
    (log/info "Starting ETL")
    (login! this) ; Will throw on error
    (log/info "Login successful")
    (assoc this :keep-session (start-keep-session-service this)))
  (stop [this]
    (log/info "Stopping ETL")
    (logout! this)
    (log/info "Logout successful")
    (stop-keep-session-service keep-session)
    this))

;; Same as for plain etl
(defn make-login-etl [params] (map->LoginETL {:params params}))
