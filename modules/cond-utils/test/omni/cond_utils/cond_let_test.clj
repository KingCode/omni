(ns omni.cond-utils.cond-let-test
  (:require [omni.cond-utils.cond-let :refer [cond-let cond-let>]]
            [clojure.test :as t :refer [deftest testing is are]]))


(defn cond-let-harness [n]
  (cond-let 
   (odd? x)
   [x n]
   (inc x)

   (< n 10)
   [y (inc n)]
   (* 10 y)

   :else
   (+ x y)))

(deftest cond-let-test
  (testing "for basic usage, and that local let-bindings pass through")
  (are [n exp] (is (= exp (cond-let-harness n)))
    3 4
    2 30))

(defn cond-let>-harness [n]
  (cond-let> 
    (odd? x)
    [x n]
    (inc x)
    
    (< n 10)
    [y (dec n)]
    (* y n)
    
    :else n))

(deftest cond-let>-test
  (testing "basic usage"
    (are [n exp] (is (= exp (cond-let>-harness n)))
      3 4
      4 12
      20 20))
  (testing "that local let-binding don't pass through"
    (is 
     (thrown? clojure.lang.Compiler$CompilerException 
              (eval '(let [n 22]
                      (cond-let> 
                        (odd? x) [x n] (inc x)
                        (< 10 (+ y z)) [y (inc n) z 80] (* 2 n z)
                        ;; unresolvable: x
                        :else x)))))))
