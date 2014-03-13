(ns sandbox.stats.qfun
  (:import (org.apache.commons.math3.special Erf)))

(def e Math/E)
(defn pow [x n] (Math/pow x n))
(defn exp [x] (Math/exp x))
(defn sqrt [x] (Math/sqrt x))
(defn abs [x] (Math/abs x))

;; aka double factorial
(defn fact-odd [x-odd]
  (loop [accum 1
         i x-odd]
    (if (neg? i)
      accum
      (recur (* accum i) (- i 2)))))

(defn q-deriv [x i]
  (letfn [(f [n] (/
                  (* (pow -1 n) (fact-odd (dec (* 2 n))))
                  (pow x (inc (* 2 n)))))]
    (* (pow e (/ (* x x) -2)) (apply + (map f (range (inc i)))))))

(map #(vector % (q-deriv % 10)) (range 0.5 5 0.5))

(defn tau [x t]
  (* t (exp (+
             (- (* x x))
             -1.26551223
             (*  1.00002368 t)
             (*  0.37409196 t t)
             (*  0.09678418 t t t)
             (* -0.18628806 t t t t)
             (*  0.27886807 t t t t t)
             (* -1.13520398 t t t t t t)
             (*  1.48851587 t t t t t t t)
             (* -0.82215223 t t t t t t t t)
             (*  0.17087277 t t t t t t t t t)))))

;; http://en.wikipedia.org/wiki/Error_function#Numerical_approximation
(defn erf [x]
  (let [t (/ 1 (+ 1 (* 0.5 (abs x))))]
    (if (neg? x)
      (- (tau x t) 1)
      (- 1 (tau x t)))))

(defn erf [x] (Erf/erf x))

;; http://perso.ensil.unilim.fr/~meghdadi/notes/alternat_Q_function.pdf
(defn q-alt [x]
  (- (/ 1 2) (* (/ 1 2) (erf (/ x (sqrt 2))))))

;; http://en.wikipedia.org/wiki/Q-function
(def q-table
  [[0.0 0.500000000]
   [0.1 0.460172163]
   [0.2 0.420740291]
   [0.3 0.382088578]
   [0.4 0.344578258]
   [0.5 0.308537539]
   [0.6 0.274253118]
   [0.7 0.241963652]
   [0.8 0.211855399]
   [0.9 0.184060125]
   [1.0 0.158655254]
   [1.1 0.135666061]
   [1.2 0.115069670]
   [1.3 0.096800485]
   [1.4 0.080756659]
   [1.5 0.066807201]
   [1.6 0.054799292]
   [1.7 0.044565463]
   [1.8 0.035930319]
   [1.9 0.028716560]
   [2.0 0.022750132]
   [2.1 0.017864421]
   [2.2 0.013903448]
   [2.3 0.010724110]
   [2.4 0.008197536]
   [2.5 0.006209665]
   [2.7 0.003466974]
   [2.8 0.002555170]
   [2.9 0.001865813]
   [3.0 0.001349898]
   [3.1 0.000967603]
   [3.2 0.000687138]
   [3.3 0.000483424]
   [3.4 0.000336929]
   [3.5 0.000232629]
   [3.6 0.000159109]
   [3.7 0.000107800]
   [3.8 0.000072348]
   [3.9 0.000048096]
   [4.0 0.000031671]])

(defn delta [f]
  (map (fn [[x q]] [x (- q (f x))]) q-table))

(delta q-alt)

(filter (fn [[x q]] (> q 1e-8)) (delta q-alt))

(time (delta q-alt))


