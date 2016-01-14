(defproject inkle/inkle "0.1.0"
  :description "clojurescript - incremental dom integration"
  :url "http://github.com/tommichaelis/inkle"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/core.async "0.2.374"]
                 [cljsjs/incremental-dom "0.3-0"]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["target"]
)
