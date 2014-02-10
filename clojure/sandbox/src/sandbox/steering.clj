(ns sandbox.steering
  (:require [sandbox.pid :refer :all]))

;; 60 degrees per second
(def max-turn-rate (/ Math/PI 3))

;; max change in heading due to error in a second, 0.9 degrees per second
(def straight-line-error-rate (* 0.005 Math/PI ))

;; distance traveled in meters per second
(def speed 0.05)

;; samples per second, Hz
(def sample-rate 5)

;; Gain values for configuring the pid
(def gains [0.6 0.6 0.3])

;; The value the sensor would report if the black line was fully in the view
(def max-black 10)

;; Width of black line in meters (1 inch)
(def line-width 0.0254)



(defn rad->deg [radians] (* radians (/ 180 Math/PI)))

(defn input->delta-heading
  "Convert scalar steering input to a change in heading in radians.  An input
  can never turn faster than the max-turn-rate"
  [input]
  (/ (min max-turn-rate (* input max-turn-rate)) sample-rate))

(defn perturb-heading
  "Adds error in driving straight"
  [heading]
  (/ (+ (* (dec (+ (rand) (rand))) straight-line-error-rate) heading) sample-rate))

(defn dist-from-centered
  [last-dist heading]
  (let [fwd-dist (/ speed sample-rate)
        side-dist (* fwd-dist (Math/sin heading))]
    (+ last-dist side-dist)))

(defn bound [value min-val max-val]
  (cond
   (> value max-val) max-val
   (< value min-val) min-val
   :else value))

(defn sense-line
  "If centered, the right half of the black line should be seen"
  [dist-from-center]
  (let [line-in-fov (bound (+ dist-from-center (/ line-width 2)) 0 line-width) ;; 0 to 0.0254
        sensor-val (* 10.0 (- 1 (/ line-in-fov line-width)))]
    sensor-val))

(defn normalize-error
  "We want to see half of the black line.  Converts a (0, 10) sensor reading to a (-1, 1) error value"
  [sensor-reading]
  (let [half-black (/ max-black 2)]
    (/ (- sensor-reading half-black) half-black)))

(defn sim-steer
  [sim-duration]
  (loop [dist-from-center 0
         heading 0
         pid (create-pid gains 0)
         t 0]
    (let [nxt-time (+ t (/ 1 sample-rate))
          nxt-dist (dist-from-centered dist-from-center heading)
          sensor-reading (sense-line nxt-dist)
          error (normalize-error sensor-reading)
          [pid-out, nxt-pid] (pid error nxt-time)
          nxt-head (perturb-heading (+ heading (input->delta-heading pid-out)))]
      (println t dist-from-center heading sensor-reading error pid-out)
      (if (< t sim-duration)
        (recur nxt-dist nxt-head nxt-pid nxt-time)))))


(sim-steer 100)
