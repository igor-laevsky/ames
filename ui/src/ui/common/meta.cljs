(ns ui.common.meta)

;;;
;;; This namespace contains various meta information about current project.
;;;

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
