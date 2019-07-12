(ns ui.common.config)

(def ^:const api-url "http://0.0.0.0")

(defn endpoint [& params]
  (clojure.string/join "/" (concat [api-url] params)))
