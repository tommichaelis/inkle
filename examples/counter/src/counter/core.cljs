(ns counter.core
  (:require [goog.dom :as dom]
            [inkle.core :as inkle]))

(defn counter [name]
  (let [count (inkle/iatom 0)]
    (fn []
      [:div
        [:span "Clicked " @count " times"]
        [:button {:onclick #(swap! count inc)}
                 "Click Me!"]])))

(inkle/renderer (dom/getElement "app") [counter])
