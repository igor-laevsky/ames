(ns ui.common.utils
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :as re-frame]))

(defn- validate-db-helper [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

;; Creates interceptor which validates the db according to a given spec.
;; `spec` should be a keyword.
(defn validate-db [spec]
  (re-frame/after (partial validate-db-helper spec)))

;; Helper function to convert visit to it's name
;;
(defn visit->name [{:keys [name group]}]
  (let [clean-name (clojure.string/replace name "vnok/se." "")]
    (if group
      (str clean-name " (" group ")")
      clean-name)))

;; Same as above, but returns url friendly name.
;;
(defn visit->url-name [{:keys [name group]}]
  (let [clean-name (clojure.string/replace name "vnok/se." "")]
    (if group
      (str clean-name "-" group)
      clean-name)))

;; Parse visit name into the {:name "..." :group "..."} form.
;;
(defn url-name->visit [inp]
  (let [[name group] (clojure.string/split inp #"-")
        full-name (str "vnok/se." name)]
    (if group
      {:name full-name :group group}
      {:name full-name})))
