(ns sandbox.tutorial.core)

;;
;; Code as data
;;
(fn? +) ;; plus is a function:
(type +) ;; functions are first class, they have a type. plus's type is:
(+ 1 2 3) ;; a list with a function first and then the args, which evaluates to:
(read-string "(+ 1 2 3)") ;; the reader takes the string and evals to *data*:
(type (read-string "(+ 1 2 3)")) ;; the data's type is:
(eval (list + 1 2 3)) ;; the evaluator takes a list and produces a result:

;;
;; Everything is a function
;;
(def m {:a 1 :b 2}) ;; we make a symbol for a map:
(symbol? 'sandbox.tutorial.core/m)
(fn? m) ;; maps are a function:
(ifn? m) ;; ok, they arent created using "fn", but they can be used like a function:
(fn? :a) ;; are keywords objects created with fn:
(ifn? :a) ;; however, they can be invoked like functions:
(:a m)
(m :b)

(eval +)
(symbol? 'clojure.core/+)

