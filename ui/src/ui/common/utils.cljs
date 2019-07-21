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
