(ns sandbox.async.core
  (:require [stopwatch.core :refer :all]
            [clojure.core.async :as a]))

(def items 1e7)

(defn baseline []
  (loop [[v & vs] (range items)
         accum 0]
    (if v
      (recur vs (+ v accum))
      accum)))

(defn consumer [chan]
  (future
    (loop [accum 0]
      (let [v (a/<!! chan)]
        (if (= v :nil)
          accum
          (recur (+ v accum)))))))

(defn producer [chan items]
  (future
    (doseq [i items]
      (a/>!! chan i))
    (a/>!! chan :nil)))

(defn one-pair-threads []
  (let [chan (a/chan 1e5)
        cons (consumer chan)]
    (producer chan (range items))
    @cons))

(defn one-chan-two-pair-threads []
  (let [chan (a/chan 1e5)
        cons1 (consumer chan)
        cons2 (consumer chan)]
    ;(producer chan (range items))
    (producer chan (range (long (/ items 2))))
    (producer chan (range (long (/ items 2)) items))
    (+ @cons1 @cons2)))

(defn two-chan-two-pair-threads []
  (let [chan1 (a/chan 1e5)
        chan2 (a/chan 1e5)
        cons1 (consumer chan1)
        cons2 (consumer chan2)]
    ;(producer chan (range items))
    (producer chan1 (range (long (/ items 2))))
    (producer chan2 (range (long (/ items 2)) items))
    (+ @cons1 @cons2)))