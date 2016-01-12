(ns inkle.core
  (:require [inkle.hiccup :as hiccup]
            [inkle.atom :as atom]
            [inkle.incremental :as incremental]))

(def renderer (hiccup/get-renderer (incremental/strategy)))

(def iatom atom/iatom)
