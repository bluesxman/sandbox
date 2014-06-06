(ns sandbox.life.core
  (:import [java.util.concurrent Executors]
           [bluesxman.sandbox.life LifeView]))

;; See: http://en.wikipedia.org/wiki/Conway's_Game_of_Life


;;;; Setup

(def world-x 240)
(def world-y 120)

(defonce lv (LifeView/createInstance))

(def sim-threads 8)
(def sim-pool (Executors/newFixedThreadPool sim-threads))

(defn init-world [max-x max-y]
  (vec
   (repeatedly
    max-x
    (fn []
      (vec
       (repeatedly max-y (fn [] (< (rand) 0.5))))))))

(def world (atom (init-world world-x world-y)))


;;;; Rendering

(defn update-buffer! [lv event]
  (let [[x y v] event]
    (.setSquare lv x y v)))

(defn draw-frame! []
  (.render lv))

(defn erase! []
  (doseq [x (range world-x)
          y (range world-y)]
    (.setSquare lv x y false)))


;;;; Simulation

(defn state-at [w x y]
  (get-in w [x y]))

(defn neighbors [w x y]
  (let [minx (max 0 (dec x))
        miny (max 0 (dec y))
        maxx (min world-x (+ x 2))
        maxy (min world-y (+ y 2))]
    (apply + (for [i (range minx maxx)
                   j (range miny maxy)
                   :when (not (and (= i x) (= j y)))]
               (if (state-at w i j) 1 0)))))

;; Any live cell with fewer than two live neighbours dies, as if caused by under-population.
;; Any live cell with two or three live neighbours lives on to the next generation.
;; Any live cell with more than three live neighbours dies, as if by overcrowding.
;; Any dead cell with exactly three live neighbours becomes a live cell, as if by reproduction.
(defn next-state [w x y]
  (let [n (neighbors w x y)]
    (if (state-at w x y)
      (<= 2 n 3)
      (= n 3))))

(defn update! [x y v]
  (do
    (swap! world assoc-in [x y] v)
    (update-buffer! lv [x y v])))

(defn step-cell! [w x y]
  (let [old (state-at w x y)
        new (next-state w x y)]
    (when (not= old new)
      (update! x y new))))

(defn create-task! [w xs]
  (fn []
    (doseq [x xs
            y (range world-y)]
      (step-cell! w x y))))

(defn timestep! [w]
  (let [size (/ world-x sim-threads)
        parts (partition size (range world-x))
        tasks (map #(create-task! w %) parts)]
    (doseq [fut (.invokeAll sim-pool tasks)]
      (.get fut))))

(defn simulate! []
  (loop [w @world]
    (timestep! w)
    (recur @world)))


;;;; Execution

;; (def sim-threads 8)

;; (erase!)
;; (reset! world (init-world world-x world-y))

;; (def go? false)
;; (def go? true)
;; (def sleep? true)
;; (def sleep? false)
;; (def sleep-time 17)

;; (.start (Thread. (fn [] (while go? (do
;;                                      (timestep! @world)
;;                                      (draw-frame!)
;;                                      (when sleep? (Thread/sleep sleep-time)))))))


