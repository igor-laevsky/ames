(ns ui.test.main
  (:require [ui.test.example]
            [figwheel.main.testing :refer [run-tests-async]]))

(defn -main [& args]
  (run-tests-async 5000))