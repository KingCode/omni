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

(defmacro with-dirpath
  "Creates (base-dir)/(rel-path) as a directory hierarchy (mkdir -p style),
   runs body, then deletes that subtree (rm -rf), even if body throws.

   Throws ex-info if base-dir does not exist, or exists but isn't a directory."
  [base-dir rel-path & body]
  `(let [base-dir#  ~base-dir
         full-path# (fs/path base-dir# ~rel-path)
         delete-root# (fs/path base-dir# ~(-> rel-path split-path first))]
     (cond
       (not (fs/exists? base-dir#))
       (throw (ex-info "Directory does not exist" {:base-dir base-dir#}))

       (not (fs/directory? base-dir#))
       (throw (ex-info "Path exists but is not a directory" {:base-dir base-dir#})))
     (fs/create-dirs full-path#)
     (try
       ~@body
       (finally
         (println "DELETING" delete-root#)
         (fs/delete-tree delete-root#)))))

(defmacro within-test-resources-dir [rel-path & body]
  `(with-dirpath 
       (fs/path (super-root) "modules/build-utils/test-resources")
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


