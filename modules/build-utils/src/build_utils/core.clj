(ns build-utils.core
  (:require [build-utils.util :as u]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))

(defn default-root-dir []
  (System/getProperty "user.dir"))

(defn user-dir []
  (default-root-dir))

(def default-class-subdir (u/join-path "target" "classes"))

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

  Use `mark-sandbox-config` and `mark-strict-config` to signal that functions should 
  check that user.dir, root-dir and class-dir are in the same hierarchy.
"
  ([lib version root-dir class-dir]
   (let [root-dir (or root-dir (default-root-dir))
         class-dir (or class-dir (default-class-subdir))]
     {:lib lib
      :version version
      :root-dir root-dir
      :class-dir (u/join-path root-dir class-dir)
      ;; for convenience or error-checking
      :user-dir (user-dir)}))
  ([lib version root-dir]
   (create-config lib version root-dir default-class-subdir))
  ([lib version]
   (let [root-dir (default-root-dir)]
     (create-config lib version root-dir default-class-subdir root-dir))))

(defn mark-sandboxed-config
  "Marks a config as sandbox safe, i.e. that 'user dir' (the directory from which
  the java process started must strictly contain both root-dir and class-dir;
  project-dir and target-dir are allowed; project-dir and target-dir 
  do not need to be related otherwise.
"
  [config]
  (assoc config :sandboxed? true))

(defn mark-strict-config
  "Marks a config as strict, the highest level of safety, where 'user dir' must
  strictly contain both root-dir and class-dir, and root-dir must strictly contain
  class-dir

  This is expected and recommended, when used by client code.
"
  [config]
  (assoc config :strict? true))

(defn sandbox-violation?
  "Returns true if one or both of project-dir and class-dir are outside of the 
   user directory structure.  
  ."
  [{:keys [user-dir root-dir class-dir sandboxed?] :as config}]
  (or (not (u/strict-subpath? user-dir root-dir))
      (not (u/strict-subpath? user-dir class-dir))))

(defn strict-violation?
  [{:keys [root-dir class-dir strict?] :as config}]
  (or (sandbox-violation? config)
      (not (u/strict-subpath? root-dir class-dir))))

(defn violation-info [{:keys [user-dir root-dir class-dir]}]
  {:user-dir (u/normalize user-dir)
   :root-dir (u/normalize root-dir)
   :class-dir (u/normalize class-dir)})

(defn check-sandbox-violation [{:keys [sandboxed?] :as config}]
  (when (and sandboxed? (sandbox-violation? config))
    (throw (ex-info 
            "root-dir and class-dir must be children of user-dir"
            (violation-info config)))))

(defn check-strict-violation [{:keys [strict?] :as config}]
  (when (and strict? (strict-violation? config))
    (throw 
     (ex-info
      "The containment hierarchy user-dir >= root-dir > class-dir is not respected",
      (violation-info config)))))

