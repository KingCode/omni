(ns build-utils.util
  (:import (java.nio.file Paths Path Files)
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

(defn xf-binding [sym xf]
  `[~sym (~xf ~sym)])

(defmacro let-with-bindings
  "Emits a let binding symbols and one-arg calls to transformations on them,
   around `body` e.g. 

  (let-with-bindings [a? b?] boolean (cond a? \"a\" b? \"b\" :else \"other\"))
  
  yields

      (let [a? (boolean a?) b? (boolean b?)] ...)
 "
  [syms xf & body]
  (let [binds (->> syms 
                   (map #(xf-binding % xf))
                   (apply concat))]
    `(let [~@binds]
       ~@body)))

(defn exists? [^:Path path]
  (Files/exists path (into-array java.nio.file.LinkOption [])))
