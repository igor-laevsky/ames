(ns ui.common.utils
  (:require [cljs.spec.alpha :as s]))

(defn- validate-helper [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(defn validate-db [] true)