(ns inkle.atom
  (:require [clojure.set :as s]))


;; Atoms will update this when they're derefed.
;; By binding it, we can check what's derefed in a function
(declare ^:dynamic *ratom-context*)

(defn- ^number arr-len [x]
    (if (nil? x) 0 (alength x)))

(defn- ^boolean arr-eq [x y]
  (let [len (arr-len x)]
    (and (== len (arr-len y))
         (loop [i 0]
           (or (== i len)
               (if (identical? (aget x i) (aget y i))
                 (recur (inc i))
                 false))))))

(defn in-context [context f]
  (binding [*ratom-context* context]
    (f)))

;; Run the function. Capture which ratoms it derefs, so we can re-run it when these change
(defn deref-capture [f r]
  (set! (.-captured r) nil)
  (let [res (in-context r f) ;; Stores dereffed atoms in r
        captured (.-captured r)] ;; Atoms that were dereffed
    (set! (.-dirty? r) false)
    (when-not (arr-eq captured (.-watching r)) ;; If that's different from what we had before...
      (._update-watching r captured)) ;; ... update them.
    res))

;; If we have a currently active context, let it know we were dereffed.
(defn- notify-deref-watcher! [derefed]
  (when-some [r *ratom-context*]
    (let [c (.-captured r)]
      (if (nil? c)
        (set! (.-captured r) (array derefed))
        (.push c derefed)))))

(defn notify-w [a oldval newval]
  (doseq [[key f] (.-watches a)]
    (f key a oldval newval)))

(defn add-w [a key f]
  (set! (.-watches a) (assoc (.-watches a) key f))
  a)

(defn remove-w [a key]
  (set! (.-watches a) (dissoc (.-watches a) key)))

;;; Queueing
(defn fake-raf [f]
  (js/setTimeout f 16))

(def next-tick
  (let [w js/window]
    (or (aget w "requestAnimationFrame")
        (aget w "webkitRequestAnimationFrame")
        (aget w "mozRequestAnimationFrame")
        (aget w "msRequestAnimationFrame")
        fake-raf)))

(defonce ^:private queue nil)

(defn flush! []
  (loop []
    (let [q queue]
      (when-not (nil? q)
        (set! queue nil)
        (doseq [r q]
          (._run r))
        (recur)))))

(defn enqueue [r]
  (when (nil? queue)
    (set! queue (array))
    (next-tick flush!))
  (.push queue r))

(defn- handle-reaction-change [this sender old new]
  (._handle-change this sender old new))

;; Our atom. It needs to notify the watcher when it's dereffed
(deftype InkAtom [^:mutable state meta validator ^:mutable watches]
  Object
  (equiv [this other]
    (-equiv this other))

  IAtom

  IEquiv
  (-equiv [o other] (identical? o other))

  IDeref
  (-deref [this]
    (notify-deref-watcher! this)
    state)

  IReset
  (-reset! [a new-value]
    (when-not (nil? validator)
      (assert (validator new-value) "Validator rejected reference state"))
    (let [old-value state]
      (set! state new-value)
      (when-not (nil? watches)
        (notify-w a old-value new-value))
      new-value))
  
  ISwap
  (-swap! [a f]          (-reset! a (f state)))
  (-swap! [a f x]        (-reset! a (f state x)))
  (-swap! [a f x y]      (-reset! a (f state x y)))
  (-swap! [a f x y more] (-reset! a (apply f state x y more)))

  IMeta
  (-meta [_] meta)

  IWatchable
  (-notify-watches [this oldval newval] (notify-w this oldval newval))
  (-add-watch [this key f] (add-w this key f))
  (-remove-watch [this key] (remove-w this key))

  IHash
  (-hash [this] (goog/getUid this)))

(defn iatom [state]
  (InkAtom. state nil nil nil))


(defprotocol IRunnable
  (run [this]))

(defprotocol IDisposable
    (dispose! [this]))

(deftype Reaction [f ^:mutable disposed? ^:mutable state ^:mutable ^boolean dirty? ^:mutable auto-run
                   ^:mutable watching ^:mutable watches]
  IAtom

  IEquiv
  (-equiv [o other] (identical? o other))

  IMeta
  (-meta [_] meta)

  IWatchable
  (-notify-watches [this oldval newval] (notify-w this oldval newval))
  (-add-watch [this key f] (add-w this key f))
  (-remove-watch [this key]
    (let [was-empty (empty? watches)]
      (remove-w this key)
      (when (and (not was-empty)
                 (empty? watches))
        (dispose! this))))

  IDeref
  (-deref [this]
    (let [non-reactive (nil? *ratom-context*)] ;; Not running inside another reaction
      (if non-reactive
        (do (flush!)
            (when dirty?
              (let [oldstate state]
                (set! state (f))
                (when-not (or (nil? watches) (= oldstate state))
                  (notify-w this oldstate state)))))
        (do (notify-deref-watcher! this)
            (when dirty?
              (._run this))))

      state))

  IRunnable
  (run [this]
    (flush!)
    (._run this))


  IDisposable
  (dispose! [this]
    (let [s state
          wg watching]
      (set! watching nil)
      (set! state nil)
      (set! auto-run nil)
      (set! dirty? true)
      (set! disposed? true)
      (doseq [w (set wg)]
        (-remove-watch w this))
      (when (some? (.-on-dispose this))
        (.on-dispose this s))))
  
  IHash
  (-hash [this] (goog/getUid this))

  Object
  (_handle-change [this sender oldval newval]
    (when-not (or (identical? oldval newval)
                  dirty?)
      (set! dirty? true)
      (if (true? auto-run)
        (._run this)
        (enqueue this))))

  (_update-watching [this derefed]
    (let [new (set derefed)
          old (set watching)]
      (set! watching derefed)
      (doseq [w (s/difference new old)]
        (-add-watch w this handle-reaction-change))
      (doseq [w (s/difference old new)]
        (-remove-watch w this))))

  (_run [this]
    (let [oldstate state
          res (deref-capture f this)]
      (set! state res)
      (when-not (or (nil? watches)
                    (= oldstate res))
        (notify-w this oldstate res))
      res)))

(defn reaction
  ([f] (reaction f false))

  ([f auto-run] (Reaction. f false nil true auto-run nil nil)))
