(ns crawler.etl
  (:require [clojure.core.async :as a]
            [clojure.tools.logging :as log]
            [clojure.core.specs.alpha :as spec]
            [com.stuartsierra.component :as component]
            [clj-time.format :as time-fmt]
            [clj-time.core :as time]

            [crawler.network :as net]
            [crawler.extractor :as extr]
            [crawler.saver :as s]
            [clojure.data.json :as js]))

;;; Extracts all exps in a given site and saves them using 'saver'.
;;;

;; Commonly used URL's here. Accept ETL as their first parameter.
(defn- login-url [{{:keys [main-url]} :params}]
  (str main-url "MainService.asmx/LoginUser"))
(defn- subject-matrix-url [{{:keys [main-url]} :params}]
  (str main-url "SubjectMatrix.aspx"))
(defn- keep-session-url [{{:keys [main-url]} :params}]
  (str main-url "keepsession.aspx?rnd=" (rand)))
(defn- visit-url [{{:keys [main-url]} :params} visit]
  (str main-url "SubjectMatrix.aspx?eOID=" visit))
(defn- exp-url [{{:keys [main-url]} :params} id]
  (str main-url "MainService.asmx/GetFormView?formId=" (str id)))

;; Get request for the subject matrix. Returns promise chanel.
(defn subject-matrix [{:keys [network] :as etl}]
  (net/get network (subject-matrix-url etl)))

;; Returns list of visits for the current center. Blocks caller thread.
;; Throws on error.
(defn get-visits! [etl]
  (let [{:keys [status body] :as resp} (a/<!! (subject-matrix etl))]
    (if (not= status 200)
      (throw (ex-info "Failed to receive subject matrix" resp))
      (try
        (extr/extract-visits body)
        (catch Exception e
          (throw (ex-info "Failed to parse subject matrix" resp e)))))))

;; Spawns a go block which receives chanel with visits and pushes list of exps
;; to the output chanel. Each exp has the form according to the extr/extract-exps
;; i.e ({:id "1234", :context {:rand-num "R123"}} ...)
;; Closes to-chan once from-chan is closed.
(defn start-visit-exp-service [etl from-chan to-chan]
  (a/go
    (log/info "Starting visit-exp service")
    (try
      (loop []
        (if-some [v (a/<! from-chan)]
          (do
            (log/info "Parsing visit " v)
            (let [url (visit-url etl v)
                  {:keys [status body] :as v-resp} (a/<! (net/get (:network etl) url))]
              (if (not= status 200)
                (log/warn "Failed to request visit " v-resp)
                (a/<! (a/onto-chan to-chan (extr/extract-exps body) false))))
            (recur))
          (do
            (a/close! to-chan)
            (log/info "Exiting from the visit-exp service"))))
      (catch Throwable e
        (log/warn "Failure in the visit-exp service " e)
        (log/warn "Restarting visit-exp service after failure")
        (start-visit-exp-service etl from-chan to-chan)))))

;; Spawns a go block which receives exps id's, requests them from the server
;; and parses into data structures conforming to the ::cdl/exp spec.
;; Closes to-chan once from-chan is closed.
(defn start-parse-exp-service [etl from-chan to-chan]
  (a/go
    (log/info "Starting parse-exp service")
    (try
      (loop []
        (if-some [e (a/<! from-chan)]
          (do
            (log/info "Parsing exp " e)
            (let [url (exp-url etl (:id e))
                  {:keys [status body] :as e-resp}
                  (a/<! (net/get (:network etl) url
                                 {:content-type :json}))]
              (if (not= status 200)
                (log/warn "Failed to request exp " e-resp)
                (if-some [exp (extr/extract-exp body (:context e))]
                  (do
                    (log/info "Successfully parsed exp " e)
                    (a/>! to-chan exp))
                  (log/info "No parser for the exp " e))))
            (recur))
          (do
            (a/close! to-chan)
            (log/info "Exiting from the parse-exp service"))))
      (catch Throwable err
        (log/warn "Failure in the parse-exp service " err)
        (log/warn "Restarting parse-exp service after failure")
        (start-parse-exp-service etl from-chan to-chan)))))

;; Spawns a go block which expects stream of parsed exp's and saves them using
;; current etl saver.
(defn start-save-exp-service [etl from-chan]
  (a/go
    (log/info "Starting save-exp service")
    (try
      (loop []
        (if-some [e (a/<! from-chan)]
          (do
            (log/info "Saving exp " (select-keys e [:patient :type :visit]))
            (try
              (s/save (:saver etl) e)
              (catch Throwable err
                (log/info "Error while saving exp " err)))
            (recur))
          (log/info "Exiting from the save-exp service")))
      (catch Throwable e
        (log/warn "Failure in the save-exp service " e)
        (log/warn "Restarting save-exp service after failure")
        (start-save-exp-service etl from-chan)))))

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
(defn keep-session! [etl]
  (net/get! (:network etl) (keep-session-url etl)))

(defn start-keep-session-service [etl]
  (let [close-chan (a/chan)]
    (a/thread
      (loop []
        (let [[_ res] (a/alts!! [close-chan (a/timeout 500000)])]
          (when-not (= res close-chan)
            (log/info "Sending keep session request")
            (keep-session! etl)
            (recur)))))
    close-chan))

(defn stop-keep-session-service [service]
  (a/close! service))

;; Logout current user. Blocks caller thread.
(defn logout! [etl]
  (call-server-menu! etl "logout"))

;; ETL which doesn't do anything on start and stop.
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

;; Parses all exps from the current center and saves them into saver.
(defn parse-center! [etl & {:keys [num-parser-threads] :or {num-parser-threads 1}}]
  (let [visits-chan (a/to-chan (get-visits! etl))
        exps-chan (a/chan)
        parsed-exps-chan (a/chan)]
    (start-visit-exp-service etl visits-chan exps-chan)
    (dotimes [_ num-parser-threads]
      (start-parse-exp-service etl exps-chan parsed-exps-chan))
    (a/<!! (start-save-exp-service etl parsed-exps-chan))))
