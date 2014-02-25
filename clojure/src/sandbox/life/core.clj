(ns sandbox.life.core
  (:require [clojure.core.async :refer :all]))

(def pipeline-buffer 100)

(def world-x 190)
(def world-y 120)

(def sim-threads 4)


(defn init-world [max-x max-y]
  (vec
   (repeatedly max-x
               (fn [] (vec
                       (repeatedly max-y (fn [] (< (rand) 0.5))))))))

(defn update! [world x y v]
  (do
    (swap! world update-in [x y] v)
    (>!! render [x y v])))

(defn step-cell [[w x y]]
  (let [old (state w x y)
              new (next w x y) ]
      (when (not= old new)
        (update! x y new))))

(defn timestep [w])

(defn simulate [pipeline max-x max-y]
  (let [world (atom (init-world max-x max-y))])
  (loop [w @world]
    (timestep w)
    (recur @world)))

(defn render [pipeline]
  ;; init the graphics
  ;; loop on the pipeline, rendering any items to the buffer as they arrive
  ;; use a timeout to make the buffer visible at 60 FPS
)

(defn -main []
  (let [pipeline (chan pipeline-buffer)]
    (simulate pipeline world-x world-y)
    (render pipeline)))
