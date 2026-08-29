(ns build-utils.config
  (:require [build-utils.util :as u]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defn user-dir []
  (System/getProperty "user.dir"))

(defn default-root-dir []
  (user-dir))

(def default-target-dir "target")

(defn create-config
"Creates a configuration map to be passed to all functions in this namespace
  Arguments must be as follows:
    - lib: a namespace-qualified symbol for the published artifact to be produced

    - version: a string, either the version itself or a file path 
               relative to `root-dir`

    - root-dir: a string, the top-level directory of the project for the lib,
      either an absolute path or a path relative to the directory of the current
      java process; defaults to java environment property \"user.dir\"

    - target-dir: a string, the directory containing compiled classes and other
      artifacts, relative to `root-dir`,defaults to \"target\". The target dir 
      should be created and used only by the build scripts (client code), as it 
      will be overwritten and deleted. Choose with care!

  In the returned map, all paths are absolute and normalized.

  Use `mark-sandbox-config` and `mark-strict-config` to signal that functions should 
  check that user.dir, root-dir and target-dir are in the same hierarchy.
"
  ([lib version root-dir tgt-dir]
   (let [root-dir (or root-dir (default-root-dir))
         tgt-dir (or tgt-dir (default-target-dir))]
     {:lib lib
      :version version
      :root-dir (-> root-dir u/normalize)
      :target-dir (u/join-path root-dir tgt-dir)
      ;; for convenience or error-checking
      :user-dir (user-dir)}))
  ([lib version root-dir]
   (create-config lib version root-dir default-target-dir))
  ([lib version]
   (let [root-dir (default-root-dir)]
     (create-config lib version root-dir default-target-dir root-dir))))

(defn mark-as-sandboxed
  "Marks a config as sandbox safe, i.e. that 'user dir' (the directory from which
  the java process started must strictly contain both root-dir and target-dir;
  project-dir and target-dir are allowed; project-dir and target-dir 
  do not need to be related otherwise.
"
  [config]
  (assoc config :sandboxed? true))

(defn mark-as-strict
  "Marks a config as strict, the highest level of safety, where 'user dir' must
  strictly contain both root-dir and target-dir, and root-dir must strictly contain
  target-dir

  This is expected and recommended, when used by client code.
"
  [config]
  (assoc config :strict? true))

(defn mark-exluded-dirs
  "Marks dir names which must not be part target-dir's pathname, e.g. \"src\"."
  [config dirname1 & dirnames]
  (assoc config :exclude (->> dirnames (cons dirname1) vec)))

(defn sandbox-violation?
  "Returns true if one or both of project-dir and target-dir are outside of the 
   user directory structure.  
  ."
  [{:keys [user-dir root-dir target-dir sandboxed?] :as config}]
  (or (not (u/strict-subpath? user-dir root-dir))
      (not (u/strict-subpath? user-dir target-dir))))

(defn strict-violation?
  [{:keys [root-dir target-dir strict?] :as config}]
  (or (sandbox-violation? config)
      (not (u/strict-subpath? root-dir target-dir))))

(defn excluded-violation? 
  [{:keys [target-dir excluded]}]
  (and excluded 
       (->> target-dir 
            u/path-elems
            (some (set excluded)))))

(defn violation-info [{:keys [user-dir root-dir target-dir excluded]}]
  {:user-dir (u/normalize user-dir)
   :root-dir (u/normalize root-dir)
   :target-dir (u/normalize target-dir)
   :excluded excluded})

(defn check-sandbox-violation [{:keys [sandboxed?] :as config}]
  (when (and sandboxed? (sandbox-violation? config))
    (throw (ex-info 
            "root-dir and target-dir must be children of user-dir"
            (violation-info config)))))

(defn check-strict-violation [{:keys [strict?] :as config}]
  (when (and strict? (strict-violation? config))
    (throw 
     (ex-info
      "The containment hierarchy user-dir >= root-dir > target-dir is not respected",
      (violation-info config)))))

(defn check-excluded-violation 
  [{:keys [strict? sandboxed? excluded target-dir]
    :as config}]
  (when (and (or sandboxed? strict?) excluded excluded-violation?)
    (throw 
     (ex-info
      "The target directory contains an excluded directory!"
      (violation-info config)))))
