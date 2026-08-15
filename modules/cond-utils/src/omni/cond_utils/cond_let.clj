(ns omni.cond-utils.cond-let)
;; (cond-let 
;;   (odd? x) [x n] (inc x)  
;;   (< n 10) [y (inc n)] 10
;;   :else n))

;; we want the above to yield
;; (let [x n]
;;   (if (odd? x)
;;      (inc x)
;;      (let [y (inc n)]
;;           (if (< n 10)
;;               10
;;               (if :else
;;                   n
;;                   (throw ...."no matching clause"))))))

(defmacro cond-let 
  "Takes ternary clauses which can use bindings visible to both the test 
  and result expression, as well as all following clauses (except when shadowed);
  the last clause which can be binary and follows 'cond semantics. 
  Each ternary clause can be of the form
        text-expr binding-vector result-expr 
  or 
        test-expr :>> result-expr 
  if there are no new bindings added to the clause
  (:>> is an ordinary keyword) 
  "
  [& clauses]
  (let [emit (fn emit [args]
               (let [[[pred binds expr :as clause] more]
                     (split-at 3 args)
                     n (count args)]
                 (cond
                   (= n 0) `(throw (IllegalArgumentException. 
                                    (str "No matching clause: " ~expr)))
                   (< n 2) `(throw (IllegalArgumentException. 
                                    (str "Must have at least 2 arguments: " 
                                         ~@clause)))
                   (= n 2)
                   `(if ~pred 
                      ~(second clause)
                      ~(emit more))
                   (= :>> (second clause))
                   `(if ~pred
                      ~expr
                      ~(emit more))
                   :else
                   `(let ~binds
                      (if ~pred 
                        ~expr
                        ~(emit more))))))]
    (emit clauses)))


;; (cond-let> 
;;     (odd? x) [x n] (inc x)
;;     (even? n) :>> (dec n) 
;;     (< 10 (+ y z)) [y (inc n) z 80] (* 2 n z)
;;     :else n

;; we want the above to yield:
;;     (or (let [x n]
;;           (when (odd? x)
;;             (inc x))
;;         (when (even? n)
;;           (dec n))))
;;         (let [y (inc n) z 80]
;;           (when (< 10 (+ y z))
;;             (* 2 n z)))
;;         (when :else
;;           n)
;;         (throw..."No matching clause.."))


(defmacro cond-let> 
  "Same as for cond-let, except bindings are local to each clause only."
  [& clauses ]
  (let [emit (fn emit [args]
               (let [[[pred binds expr :as clause] more]
                     (split-at 3 args)
                     n (count args)]
                 (cond 
                   (= n 0) [`(throw (IllegalArgumentException. 
                                     (str "No matching clause: " ~expr)))]
                   (< n 2) [`(throw (IllegalArgumentException. 
                                     (str "Must have at least 2 arguments, only got: " 
                                          ~@clause)))]
                   (= n 2)
                   (cons `(when ~pred
                           ~(second clause))
                         (emit more))
                   (= :>> (second clause))
                   (cons `(when ~pred
                            ~(last clause))
                         (emit more))
                   :else
                   (cons `(let ~binds
                            (when ~pred 
                              ~expr))
                         (emit more)))))]
    `(or ~@(emit clauses))))


(defn cond-let-sample [n]
  (cond-let 
   (neg? x) [x n] (inc x)
   (even? n) :>> (* (quot x n)  (dec n)) 
   (< 9 (+ y z)) [y (inc n) z 3] (* 2 n z)
   (= 7 (+ y z)) :>> "reused binding from previous clause"
   :else n))

(cond-let-sample -3) ;; => -2 
(cond-let-sample 34) ;; => 33 
(cond-let-sample 7) ;; => 42  
(cond-let-sample 3) ;; => 3 
(cond-let-sample 3) ;; =? "reused binding from previous clause"

(defn cond-let>-sample [n]
  (cond-let> 
   (neg? x) [x n] (inc x)
   (even? n) :>> (dec n) 
   (< 10 (+ y z)) [y (inc n) z 3] (* 2 n z)
   :else n)
  )

(comment 
  (cond-let>-sample -3) ;; => -2

  (cond-let>-sample 34) ;; => 33
  
  (cond-let>-sample 7) ;; => 42
  
  (cond-let>-sample 3) ;; => 3 

  ;; cond-let> clauses don't nest bindings, uncomment to try:
  #_(cond-let> 
     (odd? x) [x 2] x
     (even? x) :>> "even steven" ;; can't reuse higher up bindings!
     :else :whatever)
  ;; => ...Unable to resolve symbol: x in this context


  )
