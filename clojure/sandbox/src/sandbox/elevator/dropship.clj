(ns sandbox.elevator.dropship
  (:require [ring.adapter.jetty :refer :all]
            [compojure.core :refer :all]
            [clojure.core.async :as async]))
(import '(java.util.concurrent Executors))

;;;;;;;;;;;;;;;;;;;;;
;; Configuration
;;;;;;;;;;;;;;;;;;;;;

(def num-floors 6)
(def num-plan-threads 4)


;;;;;;;;;;;;;;;;;;;;;
;; Data;
;;;;;;;;;;;;;;;;;;;;;

(def empty-path {:path [(int (/ num-floors 2))] :cost 0})
(def best-path (ref default-path))

(def start-world
  {:calls (vec (repeat num-floors [0 :none])) ;; WARN: we're only recording the last direction!
   :gotos (vec (repeat num-floors 0))
   :floor 0
   :door :closed})
(def world (atom start-world))
(def world-chan (async/chan))

(def open-cmd "OPEN")
(def close-cmd "CLOSE")
(def up-cmd "UP")
(def down-cmd "DOWN")


;;;;;;;;;;;;;;;;;;;;;
;; Functions
;;;;;;;;;;;;;;;;;;;;;

(defn update-world [f & args]
  (async/thread (async/>!! world-chan (swap! world f args))))

(defn door-open? [w]
  (= (w :door) :open))

(defn exiting? [w]
  (zero? (get-in w [:gotos (w :floor)])))

(defn leave []
  (swap! world assoc :door :closed)
  (close-cmd))

(defn at-goal? [w p]
  (= (w :floor) (first p)))

(defn arrive []
  (dosync (alter best-path rest))
  (swap! world assoc :door :open)
  (open-cmd))

(defn travel [w]
  (dosync
   (let [goal (first @best-path)]
     (if (> goal (w :floor))
       (do
         (swap! world update-in [:floor] inc)
         (up-cmd))
       (do
         (swap! world update-in [:floor] dec)
         (down-cmd))))))

(defn next-cmd []
  (let [w @world          ;; Take a snapshot of the world and reason on it
        p @best-path]     ;; we know p was made for this world
    (if (door-open? w)
      (if (exiting? w)
        open-cmd ;; just repeat open until all passengers exit
        (leave)) ;; WARN: not handling special case of no calls AND no gotos
      (if (at-goal? w p)
        (arrive)
        (travel w)))))

(defn reset [qs]
  (reset! world start-world)
  (reset! best-path empty-path))

;; WARN: need to make call and go force the planners to stop and use the new world
(defn call [qs]
  (let [[_ fs ds] (re-find #"atFloor=(\d+)&to=(\S+)" qs)
        floor (read-string fs)
        dir (if (= "UP" ds) :up :down)
        update (fn [[cnt d]] (vec (inc cnt) dir))]
    (swap! world #(update-in % [:calls floor] update))))

(defn go [qs]
  (let [[_ floor-str] (re-find #"floorToGo=(\d+)" qs)
        floor (read-string floor-str)]
    (swap! world #(update-in % [:gotos floor] inc))))

(defn enter []
  (swap! world #(update-in % [:calls (% :floor) 0] dec)))

(defn exit []
  (swap! world #(update-in % [:gotos (% :floor)] dec)))

(defroutes handle-requests
  (GET "/nextCommand" [] (next-cmd))
  (GET "/call" {qs :query-string} (call qs) "")
  (GET "/go" {qs :query-string} (go qs) "")
  (GET "/userHasEntered" [] (enter) "")
  (GET "/userHasExited" [] (exit) "")
  (GET "/reset" {qs :query-string} (reset qs) ""))

;; when nothing to do, goto middle and stay closed
;; when empty and called, goto call floor
;; when occupied take shortest path to all destinations (traveling salesman)
;; but also open for any calls along the route.
;; when arrive open remove that floor from destinations and recalc shortest path
;;
;; Possible future considerations:
;;   - Could pay off to pick a longer path that has calls along it that want to go in
;;   the right direction
;;   - Probably need to wait for the enter/exit events before closing the door

(defn submit-world
  [w tp]
  ())

(defn plan [wc]
  (loop [tp (Executors/newFixedThreadPool num-plan-threads)
         w (async/<!! wc)]
    (if w
      (recur (submit-world w tp) (async/<!! wc)))))


(defn next-stop [up])

(defn plan-simple [wc]
  (loop [w (async/<!! wc)]
    (if w
      (let [floor (w :floor)
            above (+ (count-calls :above w) (coumt-gotos :above w))
            below (+ (count-calls :below w) (count-gotos :below w))]
        (next-stop (if (> above below)))
        (recur (submit-world w tp) (async/<!! wc))))))



;;;;;;;;;;;;;; Testing

(def server (run-jetty handle-requests {:port 9090 :join? false}))

(.stop server)

(eval @world)
(eval @best-path)





;;;;;;;; old omnibus stuff and experiments with ring

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

;; GET /call?atFloor=[0-5]&to=[UP|DOWN]
;; GET /go?floorToGo=[0-5]
;; GET /userHasEntered
;; GET /userHasExited
;; GET /reset?cause=information+message

(defroutes app
  (GET "/nextCommand" [] (next-command-handler))
  (GET "/call" {qs :query-string} (println "call " qs))
  (GET "/go" {qs :query-string} (println "goto floor " qs))
  (GET "/userHasEntered" [] (println "user has entered"))
  (GET "/userHasExited" [] (println "user has exited"))
  (GET "/reset" {qs :query-string} (println "reset" qs) ""))

(def requests (atom []))

(defn print-requests
  [req]
  (println req))

(defn store-requests
  [req]
  (swap! requests conj req))

;; (def server (run-jetty app {:port 9090 :join? false}))
;; (def server (run-jetty print-requests {:port 9090 :join? false}))
;; (def server (run-jetty store-requests {:port 9090 :join? false}))

(.stop server)
;; (.start server)

;; (println server)

(swap! requests conj {:foo 1})
(println @requests)

(count @requests)
(@requests 2)
