(ns ui.test.main
  (:require  [figwheel.main.testing :refer [run-tests-async]]

             [ui.test.locations]
             [ui.test.patients]))

(defn -main [& args]
  (run-tests-async 5000))
