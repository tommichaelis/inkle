(ns hello-world.core
  (:require [goog.dom :as dom]
            [inkle.core :as inkle]))

(defn hello-world [name]
  [:h1#hello-world
    [:div.title "Hello " name]])

(inkle/renderer (dom/getElement "app") [hello-world "John"])

