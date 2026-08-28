(ns build-utils.util
  (:import (java.nio.file Paths)
           (java.io File))
  (:require [clojure.string :as str]))

(defn normalize [path]
  (->> (into-array String [])
       (Paths/get path)
       (.toAbsolutePath)
       (.normalize)))

(defn subpath? 
  ([parent child]
   (subpath? parent child nil))
  ([parent child already-normalized?]
   (let [p-path (if-not already-normalized? (normalize parent) parent)
         c-path (if-not already-normalized? (normalize child)  child)]
     (.startsWith c-path p-path))))

(defn strict-subpath? [parent child]
  (let [p-path (normalize parent)
        c-path (normalize child)]
    (and (subpath? p-path c-path :is-normalized)
         (not= p-path c-path))))

(defn join-path [name1 name2 & more]
  (->> more (cons name2) (cons name1)
       (str/join File/separator)))
