(ns sandbox.life.core
  (:require [clojure.core.async :refer [chan >!! <!! alts!! timeout]])
  (:import [java.util.concurrent Executors]
           [bluesxman.sandbox.life LifeView]))

;; See: http://en.wikipedia.org/wiki/Conway's_Game_of_Life


;;;; Setup

(defonce lv (LifeView/createInstance))

(def world-x 240)
(def world-y 120)

(def sim-threads 8)
(def sim-pool (Executors/newFixedThreadPool sim-threads))

(def pipeline-buffer 10000)
(def pipeline (chan pipeline-buffer))

(defn init-world [max-x max-y]
  (vec
   (repeatedly
    max-x
    (fn []
      (vec
       (repeatedly max-y (fn [] (< (rand) 0.5))))))))

(def world (atom (init-world world-x (/ world-y 2))))

;; (<!! pipeline)

(def frame-rate 60) ;; frames per second
(def frame-length (long (/ 1000 frame-rate))) ;; milli-sec

;;;;;;;;;;;;;;;;;

(defn update-buffer [lv event]
  (let [[x y v] event]
    (.setSquare lv x y v)))

(defn draw-frame []
  (.render lv))


;; init the graphics
;; loop on the pipeline, rendering any items to the buffer as they arrive
;; use a timeout to make the buffer visible at 60 FPS
(defn render []
    (loop [next-redraw 0
           [event src] [nil nil]]
      (let [redraw? (>= (System/currentTimeMillis) next-redraw)
            nxt (if redraw? (+ next-redraw frame-length) next-redraw)]
        (when (= src pipeline) (update-buffer lv event))
        (when redraw? (draw-frame))
        (recur nxt (alts!! [pipeline
                            (timeout (- nxt (System/currentTimeMillis)))])))))



;; (future simulate)

;; (future (dotimes [n 1000] (timestep @world)))

;; (loop [next-redraw (System/currentTimeMillis)
;;        event nil]
;;   (let [redraw? (>= (System/currentTimeMillis) next-redraw)
;;         nxt (if redraw? (+ next-redraw frame-length) next-redraw)]
;;     (when event (update-buffer lv event))
;;     (when redraw? (draw-frame lv))
;;     (recur nxt (<!! pipeline))))

;; (<!! pipeline)

;;     (println [event next-redraw redraw? nxt (type (- nxt (System/currentTimeMillis)))])
;; (timestep @world)

;; (def nxt (+ (System/currentTimeMillis) frame-length))
;; (type nxt)
;; (alts!! [(timeout (- nxt (System/currentTimeMillis)))])
;; (alts!! [(timeout (- (long 1000) (long 500)))])
;; (timeout 500)
;; (alts!! [pipeline])

;; (<!! pipeline)



;;;;;;;;;;;;;;;;;

;; (defn update! [x y v]
;;   (swap! world assoc-in [x y] v)
;;   (>!! pipeline [x y v]))

(defn update! [x y v]
  (do
    (swap! world assoc-in [x y] v)
    (update-buffer lv [x y v])))


(doseq [i (range 1000000)]
  (timestep @world)
  (draw-frame)
  (Thread/sleep 16))

(let [w (init-world world-x (/ world-y 2))]
  (reset! world w))

(def go? true)
(def go? false)
(def sleep? true)
(def sleep? false)
(def sleep-time 160)

(try
  (.start (Thread. (fn [] (while go? (do (timestep @world)
                                       (draw-frame)
                                       (when sleep? (Thread/sleep sleep-time)))))))
  (catch Exception e
    (str e)))

(future
  (fn [] (while go? (do (timestep @world)
                      (draw-frame)
                      (when sleep? (Thread/sleep sleep-time))))))

;; (doseq [x (range world-x)
;;         y (range world-y)]
;;   (.setSquare lv x y false))

(try
  (/ 1 0)
  (catch Exception e (.toString e)))


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
  (.start (Thread. (fn [] (simulate))))
  (render))

(-main)
