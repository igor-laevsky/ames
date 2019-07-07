(ns ui.app-server
  (:require
    [ring.util.response :refer [resource-response content-type not-found]]))

;; Ring handler to support pushy state changes in the dev mode
(defn handler [req]
  (some-> (resource-response "index.html" {:root "public"})
          (content-type "text/html; charset=utf-8")))
