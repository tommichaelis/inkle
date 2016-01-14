(defproject inkle-todomvc "0.1.0"
  :description "inkle TodoMVC"
  :url "http://github.com/tommichaelis/inkle"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/core.async "0.2.374"]
                 [inkle/inkle "0.1.0"]]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.0-1"]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]

                :compiler {:main todomvc.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/app.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           :language-in :ecmascript5}}

               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/compiled/app.js"
                           :main todomvc.core
                           :optimizations :advanced
                           :language-in :ecmascript5
                           :pretty-print false}}]}

  :figwheel {:css-dirs ["resources/public/css"]})
