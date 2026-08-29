(ns build-utils.util-test
  ;; (:import (java.nio.file Path))
  (:require [build-utils.util 
             :refer [normalize subpath? strict-subpath? join-path]]
            [clojure.test :as t 
             :refer [deftest testing is are]]
            [clojure.string :as str]))

(defn path->str--ensure-unix [normalized-path]
  (let [elems (atom [])
        iter (-> normalized-path (.iterator))]
    (while (.hasNext iter)
      (swap! elems conj (.next iter)))
    (->> @elems 
         (str/join "/")
         (str "/"))))

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
    (are [p c] (subpath? p c)
      "/a" "/a"
      "/a" "/a/b"
      "/a" "/a/b/c/../d/e/./f"
      "/" "/a"
      "/" "."))
  (testing "negating subpath? tests, i.e. child < parent"
    (are [c p] (not (subpath? c p))
      "/a" "/"
      "/a/b" "/a"
      "/a/b/c/../d/e/./f" "/a/b/c"
      "/a/b/c/d" "3/2/1")))

(deftest strict-subpath?-test
  (testing "confirming strict-subpath? tests"
    (are [p c] (strict-subpath? p c)
      "/a" "/a/b"
      "/a" "/.././a/b"
      "/" "."))
  (testing "negating strict-subpath? tests"
    (are [c-or-p p] (not (strict-subpath? c-or-p p))
      "/a" "/"
      "/a" "/a")))

