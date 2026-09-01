(ns build-utils.config
  (:require [build-utils.util :as u]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.pprint :as pp]
            [clojure.java.io :as io]
            [clojure.core.match :as m]))

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

  Optional arguments: 
  :sandboxed? if truthy, constrains root-dir and target-dir to be
              subdirs of user-dir, and throw an error if violated. 
  
  :strict? if truthy, like :sandboxed? constraints, but target-dir must also
           be a proper (strict) subdirectory of root-dir, and throw an error
           if violated; throw an error if violated.

  :no-overwrite? if truthy, target-dir must not exist; an error is thrown
                 if violated. 

  :excluded if present, a sequential of names that are not allowed on 
            target-dir's path. An error is thrown if violated.

  :dry-run? if truthy, only report on configuration constraints and compliance
            without doing any build work, or directory/file creation. No exception
            is thrown. All possible violations are reported. 

  In the returned map, all paths are absolute and normalized.

"
  ([lib version root-dir tgt-dir 
    & {:keys [sandboxed? strict? no-overwrite? excluded dry-run?] :as opts}]
   (let [root-dir (u/normalize (or root-dir (default-root-dir)))
         tgt-dir (or tgt-dir default-target-dir)]
     (->> {:lib lib
           :version version
           :root-dir (-> root-dir u/normalize)
           :target-dir (u/normalize (u/join-path root-dir tgt-dir))
           :user-dir (user-dir)}
          (merge opts))))
  ([lib version root-dir]
   (create-config lib version root-dir default-target-dir
                  :strict? :yes))
  ([lib version]
   (create-config lib version (default-root-dir) default-target-dir)))

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


(defn mark-no-overwrite 
  "Marks target-dir as overwrite protected, i.e. must not exist in filesystem."
  [config]
  (assoc config :no-overwrite? true))

(defn sandbox-violation?
  "Returns true if one or both of project-dir and target-dir are outside of the 
   user directory structure.  
  ."
  [{:keys [user-dir root-dir target-dir sandboxed?] :as config}]
  (or (not (u/strict-subpath? user-dir root-dir))
      (not (u/strict-subpath? user-dir target-dir))))

(defn strict-violation?
  [{:keys [root-dir target-dir strict?] :as config}]
  (not (u/strict-subpath? root-dir target-dir)))

(defn excluded-violation? 
  [{:keys [target-dir excluded]}]
  (and excluded 
       (->> target-dir 
            u/path-elems
            (some (set excluded)))))

(defn overwrite-violation? 
  [{:keys [target-dir overwrite-protected?] :as config}]
  (u/exists? target-dir))

(defn violation-info [{:keys [user-dir root-dir target-dir excluded]}]
  {:user-dir (u/normalize user-dir)
   :root-dir (u/normalize root-dir)
   :target-dir (u/normalize target-dir)
   :excluded excluded})

(defn pprint-violation [fail-msg fail-map & [warning?]]
  (let [pr #(pp/pprint %) 
        indent #(print "\t")#_#(pp/pprint-indent :current 4)]
    (when warning?
      (println "WARNING!"))
    (pr fail-msg) (newline)
    (pr fail-map) (newline)
    (when warning?
      (println (str "Proceeding with execution since no restriction flags"
                    " are set in config (:sandboxed?, :strict? or :excluded...")))))

(defn handle-violation
  "Reports on the command line and throws an exception depending on whether 
   a violation occurred and other parameters, and the configuration.

   Parameters:
     - failed? boolean, whether there is a violation of some kind  
     - fail-msg string, the message to print if any
     - fail-map map, the error data  
     - dry-run? boolean, if true only report; otherwise an exception is thrown 
     - ok-msg string, the message to print that no violation occurred (dry-run).
     - violation-type keyword, one of :sandbox :strict or :excluded

  When no exception is thrown, returns nil if no violation occurred, and the 
  violation type otherwise.
  "
  [failed? fail-msg fail-map ok-msg violation-type
   {:keys [strict? sandboxed? excluded no-overwrite? dry-run? 
           single-check?] :as config}]
  (let [do-throw #(throw (ex-info fail-msg fail-map))
        pr-v #(apply pprint-violation fail-msg fail-map %&)
        return (fn [] nil)
        return-v (fn [] violation-type)
        pr+return-v #(do (pr-v) (return-v))
        pr-ok #(println (str ok-msg (when single-check? " Success!\n")))
        pr-ok+return #(do (pr-ok) (return))]

    (u/let-with-bindings 
        ;; make those strictly either true or false: (boolean failed?) etc..
        [failed? dry-run? sandboxed? strict? no-overwrite?] boolean
      
      (m/match 
       [failed? dry-run? sandboxed? strict? no-overwrite? violation-type]
       ;; always report on dry-run
       [true    true        _          _         _            _   ] (pr+return-v)
       [true    false       _        true        _         :strict] (do-throw) 
       [true    false     true       false       _         :strict] (pr+return-v) 
       [true    false     true         _         _        :sandbox] (do-throw)
       ;; unlikely, but if strict? is in config, a sandbox violation
       ;; <is> a problem
       [true    false      _          true       _        :sandbox] (do-throw)
       [true    false      _            _        _        :exclude] (do-throw)
       [true    false      _            _       true      :overwrite] (do-throw)
       ;; catch-all errors
       [true     _         _            _         _            _    ] (pr+return-v)
       ;; catch-all success
       [false    _         _            _         _            _    ]
       (if single-check? (return) (pr-ok+return))))))

(defn check-sandbox-violation [config]
  (let [failed? (sandbox-violation? config)]
    (handle-violation 
     failed?
     "Root-dir and target-dir must be children of user-dir:"
     (violation-info config)
     "No sandbox violation."
     :sandbox
     config)))

(defn check-strict-violation [config]
  (let [failed?  (strict-violation? config)]
    (handle-violation 
     failed?
     "The containment hierarchy user-dir >= root-dir > target-dir is not respected:"
     (violation-info config)
     "No strict-mode violation."
     :strict
     config)))

(defn check-excluded-violation [config]
  (let [failed?  (excluded-violation? config)]
    (handle-violation 
     failed?
     (str "The target directory has the name of an excluded directory"
          " in its path. ")
     (violation-info config)
     "No excluded dir name in target's path elements."
     :excluded
     config)))

(defn check-no-overwrite-violation [config]
  (let [failed? (overwrite-violation? config)]
    (handle-violation
     failed?
     (str "The target directory already exist, and will be overwritten. ")
     (violation-info config)
     "No overwrite violation. "
     :no-overwrite
     config)))
