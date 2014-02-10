(ns bluesxman.sandbox.pid)

(defn- next-pid
  [gains tot-err last-err last-time]
  (fn [error time-measured]
    (let [[pgain, igain, dgain] gains
          delta-t (- time-measured last-time)
          p (* pgain error)
          new-tot (+ tot-err (* delta-t error))
          i (* igain new-tot)
          d (* dgain (/ (- error last-err) delta-t))
          output (+ p i d)]
      [output (next-pid gains new-tot error time-measured)])))

(defn create-pid
  "Creates a pid function which takes error and time and returns a vector of [output, fn] where fn
  is the next iteration of the pid function."
  [gains start-time]
  (next-pid gains 0.0 0.0 start-time))

(def input-errors (cons 0 (repeat 1)))

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
