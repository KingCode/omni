(ns build-utils.test-util
  (:require [build-utils.util :refer [join-path path-elems ->Path]]
            [babashka.fs :as fs]
            [clojure.walk :as w]))

(defn super-root
  "Yields the absolute, normalized path from this project's super root,
  i.e. \"../..\" if the current project is build-utils "
[]
  (-> (fs/cwd) str))

(defn is-super-root? 
  "User/tester can check that a config is not wreaking havoc by using this first.
  `path` is the absolute path of the java process's directory, and should be 
  what omni's super root dir resolves too"
  [path]
  (= path (super-root)))

(defn test-resources-tip []
  (-> (super-root) (join-path "modules/build-utils/test-resources")))

(defn split-path [path] (-> path ->Path path-elems))

(defn no-dots [path] (-> path fs/normalize str))

(defmacro with-dirpath
  "Creates (base-dir)/(rel-path) as a directory hierarchy (mkdir -p style),
   runs body, then deletes that subtree (rm -rf), even if body throws.

   Throws ex-info if base-dir does not exist, or exists but isn't a directory."
  [base-dir rel-path & body]
  `(let [rel-path# (no-dots ~rel-path)
         base-dir#  (no-dots ~base-dir)
         full-path# (no-dots (fs/path base-dir# ~rel-path))
         delete-root# (fs/path base-dir# (-> rel-path# split-path first))]
     ;; (println :FULL-PATH full-path#)
     ;; (println :BASE-DIR base-dir#)
     (cond
       (not (fs/exists? base-dir#))
       (throw (ex-info "Base directory does not exist" {:base-dir base-dir#}))

       (not (fs/directory? base-dir#))
       (throw (ex-info "Path exists but is not a directory" {:base-dir base-dir#}))

       (= base-dir# (no-dots full-path#))
       (throw (ex-info "Path is base directory, must be a subdirectory."
                       {:base-dir base-dir# :full-path full-path#})))
     (println "Creating path:\n\t" (str full-path#))
     (fs/create-dirs full-path#)
     (try
       ~@body
       (finally
         (println "Deleting directory tree:\n\t" (str delete-root#))
         (fs/delete-tree delete-root#)))))

(def testres-dir "modules/build-utils/test-resources")

(defmacro within-test-resources-dir [rel-path & body]
  `(with-dirpath 
       (str (fs/path (super-root) testres-dir))
       ~rel-path 
     ~@body))

(defmacro within-test-resources-tmpdir [tmpdir-relpath rel-path & body]
  `(with-dirpath
       (no-dots (fs/path (super-root) testres-dir ~tmpdir-relpath))
       ~rel-path
     ~@body))

(comment 
  (= (fs/cwd)  (fs/path "."))
  (instance? java.nio.file.Path (fs/cwd))
  (fs/path ".")
  (fs/exists?  (fs/cwd))
  (fs/exists? "foo")
  (->>   "" fs/list-dir (map str))
 (fs/cwd) 
  (fs/home)
  (fs/with-temp-dir [dir 
                     {:dir (fs/path (fs/cwd) "modules/build-utils/test-resources") 
                      :prefix "TMP-1_"}]
    (fs/with-temp-dir [dir2 {:dir dir :prefix "TMP_1"}]
      (println :TEMP-DIR-PATH ": " (str dir2))
      (flush)
      (Thread/sleep 10000)))
(super-root)
(macroexpand-1
 '(with-tempdir-within-test-resources "a/b/c" d "TMP" 
   (println :TMP_SUBDIR ":" (str d))
   (flush)
   (Thread/sleep 10000)))

(fs/path  (super-root) "modules/build-utils/test-resources/a/b/c" )

(macroexpand-1 
 '(with-dirpath "." "modules/build-utils/test-resources/a/b/c" 
    (println "Yo")))

(macroexpand-1 '(within-test-resources-dir "tmp/a/b/c" (println "YO")))

(within-test-resources-dir "tmp/a/b/c" (println "YO")))


