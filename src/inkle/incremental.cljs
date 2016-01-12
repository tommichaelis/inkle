(ns inkle.incremental
  (:require [inkle.hiccup :as hiccup]
            [incremental-dom :as inc]))

(defn- element-open
  [tag {:keys [key] :as attrs}]
  (apply inc/elementOpen (concat [tag
                                  (or key "")
                                  []]
                                 (mapcat identity attrs))))

(defn- element-void
  [tag {:keys [key] :as attrs}]
  (apply inc/elementVoid (concat [tag
                                  (or key "")
                                  []]
                                 (mapcat identity attrs))))

(defn- element-close
  [tag]
  (inc/elementClose tag))

(defn- text
  [text]
  (inc/text text))

(deftype IncrementalDomStrategy []
  hiccup/IRenderStrategy
  (render-element [this elem]
    (cond
      (seq? elem)
      (doseq [item elem]
        (hiccup/render-element this item))

      (and (vector? elem) (fn? (first elem)))
      (hiccup/render-fn this elem)

      (vector? elem)
      (let [[tag attrs content] elem]
        (element-open tag attrs)
        (doseq [elem content]
          (hiccup/render-element this elem))
        (element-close tag))

      :else (text elem)))

  (render-element [this elem target]
    (hiccup/attach this #(hiccup/render-element this elem) target))

  (attach [this content-fn target]
    (inc/patch target content-fn))

  (container-element [_]
    (element-void "span" {})))

(defn strategy []
  (IncrementalDomStrategy.))
