(ns sandbox.venn.core
  (:require [clojure.set :refer :all]))

(def entities #{:a :b :c :d :e})

(def a (disj entities :b :d))

(def b (disj entities :c :d))

(clojure.set/intersection a b)

(clojure.set/difference a b)

(clojure.set/union a b)

;; key = [id time]

(defn to-key [id vuid] [id vuid])

(to-key :a 123)

;; A1: A value has 1 uid that is identical across all copies

(identical? [1] [1])
(identical? a a)
(identical? 1.0 1)
(= 1.0 1)
(== 1.0 1)
(= 1/1 1)
(= 1/2 0.5)

(def all-k
  (map keyword (map str (map char (range (int \a) (int \z))))))

(def set-map
  {:a #{:d :e :b}
   :b #{:a :c :b}
   :c #{:b :d :e}
   :d #{:a :b}
   :e #{:a :b :c}})

(apply intersection (vals set-map))

(defn- fc-recur)

(defn- find-commons
  [m]
  (loop [ks (keys m)
         k (first ks)]
    (if ks
      (for [r (rest ks)]
        (conj k (recur (dissoc ))))
      k)))

(defn shared
  [m]
  (loop []))


;;   (loop [ks #{k}
;;             vs #{(m k)}
;;             entries (seq (dissoc m k))
;;             commons {}]
;;        (if entries
;;          ()
;;          commons))
