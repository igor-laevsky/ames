(defproject ames/db-api "0.1.0"
  :description ""
  :url "https://github.com/igor-laevsky/ames"
  :license {}
  :dependencies [[org.clojure/clojure        "1.10.0"]
                 [org.clojure/clojurescript  "1.10.520"]
                 [reagent "0.8.1"]
                 [re-frame "0.10.7"]]
  :aliases {"fig" ["trampoline" "run" "-m" "figwheel.main"]
            "fig:repl" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]
            "fig:min" ["run" "-m" "figwheel.main" "-O" "advanced" "-bo" "dev"]
            "fig:test" ["run" "-m" "figwheel.main" "-co" "test.cljs.edn" "-m" "ui.test.main"]}
  :profiles
  {:dev
   {:dependencies [[com.bhauman/figwheel-main "0.2.0"]
                   [binaryage/devtools "0.9.10"]]
    :resource-paths ["target"]
    :clean-targets ^{:protect false} ["target"]}})
