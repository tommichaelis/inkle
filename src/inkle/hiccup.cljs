(ns inkle.hiccup
  (:require [inkle.atom :as a]))

(def ^{:doc "Regular expression that parses a CSS-style id and class from an element name."
       :private true}
    re-tag #"([^\s\.#]+)(?:#([^\s\.#]+))?(?:\.([^\s#]+))?")

(defn as-str [input]
  (cond
    (keyword? input) (name input)
    (symbol? input) (str input)
    (string? input) input
    :else nil)) ;; TODO: throw exception

(defn- merge-attributes [{:keys [id class]} map-attrs]
  (->> map-attrs
       (merge (if id {:id id}))
       (merge-with #(if %1 (str %1 " " %2) %2)
                   (if class {:class class}))))

(defn normalize-attributes [attrs]
  (into {} (->> attrs
                (filter second)
                (map (juxt (comp as-str first) second)))))

(defn normalize-elem
  [[tag & content]]
  (let [[tag id class] (if (fn? tag)
                         [tag nil nil]
                         (rest (re-matches re-tag (as-str tag))))
        tag-attrs {:id id
                   :class (if class (.replace ^String class "." " "))}
        map-attrs (first content)]
    (if (map? map-attrs)
      [tag
       (normalize-attributes (merge-attributes tag-attrs (first content)))
       (rest content)]
      [tag (normalize-attributes tag-attrs) content])))

(defn normalize-hiccup [hiccup]
  (cond
    (seq? hiccup)
    (mapcat normalize-hiccup hiccup)

    (vector? hiccup)
    (let [[tag attrs content] (normalize-elem hiccup)]
      (list [tag attrs (mapcat normalize-hiccup content)]))

    :else (list hiccup)))


(defprotocol IRenderStrategy
  (render-element [strategy elem] [strategy elem target])
  (attach [strategy content-fn target])
  (container-element [strategy]))

(defprotocol IComponent
  (initialize! [this])
  (render! [this])
  (destroy! [this]))

(declare ^:dynamic *component-context*)

(defn in-context [context f]
  (binding [*component-context* context]
    (f)))

(deftype Component [strategy element hiccup-fn ^:mutable destroyed? ^:mutable reaction ^:mutable sub-components]
  IComponent
  (initialize! [this]
    (set! reaction (a/reaction #(render! this)))
    (._run reaction))

  (render! [this]
    (when-not destroyed?
      (doseq [component sub-components]
        (destroy! component))
      (set! sub-components nil)
      (in-context this
                  (fn [] (let [normalized (normalize-hiccup (hiccup-fn))]
                           (render-element strategy normalized element))))))

  (destroy! [this]
    (doseq [component sub-components]
      (destroy! component))
    (a/dispose! reaction)
    (set! destroyed? true)))

(defn component [strategy element hiccup-fn]
  (let [component (Component. strategy element hiccup-fn false nil nil)]
    (initialize! component)
    component))


(defn- resolve-fn [fn args]
  (let [result (apply fn args)]
    (if (fn? result)
      result
      fn)))

(defn- render-fn [strategy [fn attrs args]]
  (let [func (resolve-fn fn args)
        element (container-element strategy)
        hiccup-func #(apply func args)
        sub-component (component strategy element hiccup-func)]
    (when-some [context *component-context*]
      (let [c (.-sub-components context)]
        (if (nil? c)
            (set! (.-sub-components context) (array sub-component))
            (.push c sub-component))))))

(defn get-renderer
  [strategy]
  (fn [element hiccup]
    (component strategy element (fn [] hiccup))))
