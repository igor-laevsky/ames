(ns crawler.parser
  (:require [clojure.string :as string])
  (:import (org.jsoup Jsoup)
           (org.jsoup.nodes Document)))

;;; Collection of functions which helps to parse input html into meaningful
;;; data. Doesn't perform any network requests, only parsing of a raw html.
;;;

;; Receives input html as string. Returns map containing all view-state
;; related keys required to switch centers.
(defn extract-viewstate [input]
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
