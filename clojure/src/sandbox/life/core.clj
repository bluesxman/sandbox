(ns sandbox.life.core
  (:require [clojure.core.async :refer [chan >!! <!! alts!! timeout]]))
(import '(java.util.concurrent Executors))
(import '(bluesxman.sandbox.life LifeView))

(def lv (LifeView/createInstance))


;; (doseq [i (range 100000)]
;;   (let [w @world]
;;     (doseq [x (range world-x)
;;             y (range world-y)]
;;       (step-cell w x y)))
;;   (doseq [x (range world-x)
;;           y (range world-y)]
;;     (.setSquare lv x y (get-in @world [x y])))
;;   (.render lv))

;; (.setSquare lv 0 0 false)

;; (neighbors @world 3 0)

;; See: http://en.wikipedia.org/wiki/Conway's_Game_of_Life

(def sim-threads 4)
(def world-x 240)
(def world-y 120)

(def sim-pool (Executors/newFixedThreadPool sim-threads))

(def pipeline-buffer 100)
(def pipeline (chan pipeline-buffer))

(defn init-world [max-x max-y]
  (vec
   (repeatedly
    max-x
    (fn []
      (vec
       (repeatedly max-y (fn [] (< (rand) 0.5))))))))

(def world (atom (init-world world-x (/ world-y 2))))

;; (doseq [i (range 1000000)]
;;   (timestep @world)
;;   (.render lv))

;; (<!! pipeline)

(def frame-rate 60) ;; frames per second
(def frame-length (/ 1000 frame-rate)) ;; milli-sec

;;;;;;;;;;;;;;;;;

;; (defn init-ui []
;;   (LifeView/createInstance))

(defn update-buffer [lv event]
  (let [[x y v] event]
    (.setSquare lv x y v)))

(defn draw-frame [lv]
  (.render lv))

;; init the graphics
;; loop on the pipeline, rendering any items to the buffer as they arrive
;; use a timeout to make the buffer visible at 60 FPS
(defn render []
    (loop [next-redraw (System/currentTimeMillis)
           [event src] [nil nil]]
      (let [redraw? (>= (System/currentTimeMillis) next-redraw)
            nxt (if redraw? (+ next-redraw frame-length) next-redraw)]
        (when (= src pipeline) (update-buffer lv event))
        (when redraw? (draw-frame lv))
        (recur nxt (alts!! [pipeline
                            (timeout (- nxt (System/currentTimeMillis)))])))))


;;;;;;;;;;;;;;;;;

(defn update! [x y v]
  (swap! world assoc-in [x y] v)
  (>!! pipeline [x y v]))

(defn update! [x y v]
  (do
    (swap! world assoc-in [x y] v)
    (update-buffer lv [x y v])))

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

(defn step-cell [w x y]
  (let [old (state-at w x y)
        new (next-state w x y)]
    (when (not= old new)
      (update! x y new))))

(defn create-task [w xs]
  (fn []
    (doseq [x xs
            y (range world-y)]
      (step-cell w x y))))

(defn timestep [w]
  (let [size (/ world-x sim-threads)
        parts (partition size (range world-x))
        tasks (map #(create-task w %) parts)]
    (doseq [fut (.invokeAll sim-pool tasks)]
      (.get fut))))

(defn simulate []
  (loop [w @world]
    (timestep w)
    (recur @world)))


(defn -main []
  (.start (Thread. (simulate)))
  (render))

;; (-main)

;; (let [parts (partition (/ world-x sim-threads) (range world-x))
;;       tasks (map #(create-task @world %) parts)]
;;   (doseq [t tasks]
;;     (t)))

;; (def step-section (create-task @world 0 60))

;; (step-section)
