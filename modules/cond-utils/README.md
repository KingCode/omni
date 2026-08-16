# omni.cond-utils 

Utilities around clojure.core/cond, currently:

* omni.cond-utils/cond-let
* omni.cond-utils/cond-let>


## Installing

__Clojure CLI / deps.edn__

`org.clojars.kingnuscodus/omni.cond-utils {:mvn/version "0.1.0-PATCH"}`

__Leiningen/Boot__

`[org.clojars.kingnuscodus/omni.cond-utils "0.1.0-PATCH"]`


### Rationale

Clojure's core `cond` provides fast and expressive branching on multiple clauses. However when branches require bindings of their own that don't warrant using a separate function, code can become unwieldy. Moving all bindings above the 'cond form could be wasteful when a binding may be used only in some or one of the branches.

`cond-let` and `cond-let>` intend to hit that sweet spot where the code is still both efficient and expressive, by making available binding "sugar" within each clause (includeing the test) as needed. Otherwise they behave exactly like `clojure.core/cond`, including a default clause at the end. 

### Almost, but not quite the same. 

`cond-let` propagates each local binding to later clauses, whereas `cond-let>` throws an error if any symbol that is not resolved before the form, is not bound in the same clause that uses it. 

### Usage

```
(require '[omni.cond-utils.cond-let :as ocl]

(defn my-func [n]
    (cond-let 
        (odd? x)     ;; test, binding-aware
        [x (inc n)]  ;; binding 
        :x-is-odd    ;; branch value 

        (and (< x 20) (pos? y)) ;; test, binding-aware 
                                ;; including previous ones 
        [y (dec n)]             ;; new binding
        :x<20-and-y-positive    ;; branch value  

        (< y -20)            ;; test
        :>>                  ;; no new-binding needed here, 
                             ;;  use :>> literal
        :y-is-cold!

        :else :whatever))
        
=> (my-func 2)
=> :x-is-odd
=> (my-func 3)
=> :x<20-and-y-positive
=> (my-func -229)
=> :y-is-cold!
=> (my-func -5)
=> :whatever

