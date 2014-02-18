;; Copied from Laurent Petit's omnibus example:
;; http://laurentpetit.github.io/blog/2013/11/05/the-clojure-omnibus/

(ns sandbox.elevator.omnibus
  (:use ring.adapter.jetty
        compojure.core))

(defn make-omnibus [nb-floors]
  (let [up   (repeat (dec nb-floors) ["OPEN", "CLOSE", "UP"])
        down (repeat (dec nb-floors) ["OPEN", "CLOSE", "DOWN"])
        up-then-down (flatten (concat up down))]
    (concat ["NOTHING"] (cycle up-then-down))))

(defn tick [elevator] (rest elevator))
(defn next-command [elevator] (first elevator))

(def nb-floors 6)
(def cabin (atom (make-omnibus nb-floors)))
(defn next-command-handler []
  (let [new-state (swap! cabin tick)]
    (next-command new-state)))

(defroutes app
  (GET "/nextCommand" [] (next-command-handler))
  (GET "*"            [] ""))

;; (defn -main []
;;   (run-jetty app {:port 9090}))

