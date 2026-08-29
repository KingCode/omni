(ns build-utils.util
  (:import (java.nio.file Paths)
           (java.io File))
  (:require [clojure.string :as str]))

(defn normalize [path]
  (if (not (instance? String path))
    (throw (ex-info "Expecting a dir path string." 
                    {:input path
                     :type (type path)}))
    (->> (into-array String [])
         (Paths/get path)
         (.toAbsolutePath)
         (.normalize))))

(defn subpath? 
  ([parent child & [normalize?]]
   (let [p-path (if normalize? (normalize parent) parent)
         c-path (if normalize? (normalize child)  child)]
     (.startsWith c-path p-path))))

(defn strict-subpath? [parent child & [normalize?]]
  (let [p-path (if normalize? (normalize parent) parent)
        c-path (if normalize? (normalize child) child)]
    (and (subpath? p-path c-path)
         (not= p-path c-path))))

(defn join-path [name1 name2 & more]
  (->> more (cons name2) (cons name1)
       (str/join File/separator)))

(defn path-elems [path]
  (let [path (if (instance? String path) (normalize path) path)]
    (->> path (.iterator) iterator-seq 
         (map str)
         vec)))
