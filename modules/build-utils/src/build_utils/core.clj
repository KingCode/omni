(ns build-utils.core
  (:import (java.io File))
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))

(defn join-path [name1 name2 & more]
  (->> more (cons name2) (cons name1)
       (str/join File/separator)))

(defn default-root-dir []
  (System/getProperty "user.dir"))

(def default-class-subdir (join-path "target" "classes"))

(defn create-config
"Creates a configuration map to be passed to all functions in this namespace
  Arguments must be as follows:
    - lib: a namespace-qualified symbol for the published artifact to be produced

    - version: a string, either the version itself or a file path 
               relative to `root-dir`

    - root-dir: a string, the top-level directory of the project for the lib,
      either an absolute path or a path relative to the directory of the current
      java process; defaults to java environment property \"user.dir\"

    - classes-dir: a string, the directory containing compiled classes, relative 
      to `root-dir`,defaults to \"target/classes\"

"
  ([lib version root-dir class-dir]
   (let [root-dir (or root-dir (default-root-dir))
         class-dir (or class-dir (default-class-subdir))]
     {:lib lib
      :version version
      :root-dir root-dir
      :class-dir (join-path root-dir class-dir)}))
  ([lib version root-dir]
   (create-config lib version root-dir default-class-subdir))
  ([lib version]
   (let [root-dir (default-root-dir)]
     (create-config lib version root-dir default-class-subdir root-dir))))
