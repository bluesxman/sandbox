(ns sandbox.pid)

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
