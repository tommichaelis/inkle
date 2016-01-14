# inkle

[![Clojars Project](https://clojars.org/inkle/latest-version.svg)](https://clojars.org/inkle)

Easy [Incremental Dom](http://github.com/google/incremental-dom) rendering using [ClojureScript](http://github.com/clojure/clojurescript).

Fast dom updating with a low compiled size. Since Incremental Dom is closure compiler compatible, it adds very little overhead to your clojurescript project. The TODOMVC example is 147kb after compilation, and only 35kb gzipped.

## Examples

inkle's interface is strongly influenced by [Reagent](http://github.com/reagent-project/reagent) - all you need is a simple function which returns a [hiccup-like](https://github.com/weavejester/hiccup) data structure.

```clj
(ns hello-world.core
  (:require [goog.dom :as dom]
            [inkle.core :as inkle]))

(defn hello-world [name]
  [:h1#hello-world
    [:div.title "Hello " name]])

(inkle/renderer (dom/getElement "app") [hello-world "John"])
```

Updating is managed using atoms:

```clj
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
```

For more examples, see the `example` folder

## Status

inkle is still very early stage, and should be expected to be buggy.

Some of the key TODOs:
* Add support for incremental dom's static keys, possibly using meta data
* Improve component lifecycle handling to avoid unnecessary dom updating
* Investigate server side rendering
* Add some tests

## Influences

inkle is strongly based on Reagent, including the atom concept (which they in turn borrowed from [reflex](https://github.com/lynaghk/reflex)).

## Licence

inkle is released under the MIT licence