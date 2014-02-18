(ns sandbox.square-wave
  (:require [sandbox.pid :refer :all]))

(defn- next-proc
  [last-value]
  (fn [proc-input]
    [(+ last-value (* 0.25 proc-input)) (next-proc last-value)]))

(defn create-process
  [starting-value]
  (next-proc starting-value))

(defn simulate
  [steps set-point start-value]
  (loop [process (create-process start-value)
         pid (create-pid [1.0 1.0 0.2] 0.0)
         error (- set-point start-value)
         t 1]
    (let [[pid-out next-pid] (pid error t)
          [proc-val next-proc] (process pid-out)]
      (println t pid-out proc-val)
      (if (< t steps)
        (recur next-proc next-pid (- set-point proc-val) (inc t))))))

(simulate 40 1 0)
