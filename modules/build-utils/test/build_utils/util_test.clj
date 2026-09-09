(ns build-utils.util-test
  ;; (:import (java.nio.file Path))
  (:require [build-utils.util  :as u
             :refer [normalize subpath? strict-subpath? path-elems
                     join-path ->Path]]
            ;; [babashka.fs :as fs]
            [clojure.test :as t 
             :refer [deftest testing is are]]
            [clojure.string :as str]))


(defn path->str--ensure-unix [normalized-path]
  (->> normalized-path ->Path (.iterator) iterator-seq
       (str/join "/")
       (str "/")))

;; (defn dot+ 
  ;; "Returns the absolute path from of `relpath` appended to \".\""
;; [relpath]
  ;; (-> "." fs/canonicalize (join-path subdir)))

(deftest normalize-test
  (testing "that paths resolve to the simplest form"
    (are [murky-path expected]  
        (= expected 
           (-> murky-path normalize path->str--ensure-unix))
        "/a/b/c" "/a/b/c"
        "/a/.." "/"
        "/a/./././b" "/a/b"
        "/../." "/"
        "/a/./b/../c" "/a/c")))


(deftest subpath?-test
  (testing "confirming subpath? tests, i.e. parent >= child"
    (are [p c] (subpath? p c :do-normalize)
      "/a" "/a"
      "/a" "/a/b"
      "/a" "/a/b/c/../d/e/./f"
      "/" "/a"
      "/" "."))
  (testing "negating subpath? tests, i.e. child < parent"
    (are [c p] (not (subpath? c p :do-normalize))
      "/a" "/"
      "/a/b" "/a"
      "/a/b/c/../d/e/./f" "/a/b/c"
      "/a/b/c/d" "3/2/1")))

(deftest strict-subpath?-test
  (testing "confirming strict-subpath? tests"
    (are [p c] (strict-subpath? p c :do-normalize)
      "/a" "/a/b"
      "/a" "/.././a/b"
      "/" "."))
  (testing "negating strict-subpath? tests"
    (are [c-or-p p] (not (strict-subpath? c-or-p p :do-normalize))
      "/a" "/"
      "/a" "/a")))


(deftest path-elems-test
  (testing "path elements are accurate and from an absolute, normalized path."
    (are [path elems-vec]
        (= (->> elems-vec (map str) (map rest) (map #(apply str %))) 
           (path-elems path) )
      "/a/b/c/d" [:a :b :c :d]
      "/a/../././a/b" [:a :b])))
