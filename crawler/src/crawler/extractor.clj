(ns crawler.extractor
  (:require [clojure.string :as string]
            [clojure.data.json :as js]
            [cdl.core :as cdl]
            [clojure.spec.alpha :as s])
  (:import (org.jsoup Jsoup)
           (org.jsoup.nodes Document)))

;;; Collection of functions which help to parse input html into a meaningful
;;; data. Doesn't perform any network requests, only parsing of a raw html/json.
;;;

;; Receives input html as string. Returns map containing all view-state
;; related keys required to switch centers.
(defn extract-view-state [input]
  (let [document ^Document (Jsoup/parse input)]
    (->> ["__EVENTTARGET" "__EVENTARGUMENT" "__VIEWSTATE" "__VIEWSTATEGENERATOR"]
         (mapcat #(vector % (-> document (.select (str "#" %)) (.attr "value"))))
         (apply hash-map))))

;; Helper function which receives subject/visit matrix html as string and
;; extracts it's xmlstate as a Jsoup document.
(defn- extract-xml-state [input]
  (let [js-cpdata (-> ^Document (Jsoup/parse input)
                      (.select "#aspnetForm > script:eq(11)")
                      (.html))
        xml-start (string/index-of js-cpdata "<?xml version")
        xml-end (string/index-of js-cpdata "';" xml-start)]
    (Jsoup/parse (subs js-cpdata xml-start xml-end))))


;; Receives subject matrix as string and returns set of visits in the form of:
;; #{"visit.Name&rk=group_id" ...}
(defn extract-visits [input]
  (let [xml-matrix (extract-xml-state input)]
    (->> (.select xml-matrix "cell")
         (map #(str (.attr % "OID") "&rk=" (.attr % "RepeatKey")))
         (set))))

;; Receives visit matrix as string and returns seq of exp id's in the form of:
;; ({:id "1234", :context {:rand-num "R123"}} ...)
(defn extract-exps [input]
  (let [xml-matrix (extract-xml-state input)]
    (->> (.select xml-matrix "row")
         (mapcat
           (fn [row]
             (map
               (fn [cell]
                 (hash-map :id (.attr cell "ID")
                           :context {:rand-num (.attr row "RANDNUM")}))
               (.select row "cell[ID]")))))))

;; Receives json as string and returns clojure map conforming to the ::cdl.exp
;; spec. Context is additional information received from the 'extract-exps'.
;; Returns nil if there is no parser for this exp.
;; Throws an exception if there was a parser but it returned something which was
;; not conforming to the spec or if we failed to parse json.
(defn extract-exp [input context]
  (let [json-exp (merge (js/read-str input :key-fn keyword)
                        {:context context})]
    (when (cdl/can-parse? json-exp)
      (let [exp (cdl/json->exp json-exp)]
        (if (s/valid? ::cdl/exp exp)
          exp
          (throw (ex-info "Parsed exp didn't conform to spec "
                          {:spec-explain (s/explain-str ::cdl/exp exp)
                           :exp exp})))))))
