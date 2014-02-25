(ns sandbox.elevator.planning
  (:require [clojure.core.async :as async]))
(import '(java.util.concurrent Executors))

;;;;;;;;;;;;;;;;;;;;;
;; Data
;;;;;;;;;;;;;;;;;;;;;


(def default-plan {:path [(int (/ num-floors 2))] :cost 0 :world start-world})
(def best-plan (atom default-plan))


;;;;;;;;;;;;;;;;;;;;;
;; Functions
;;;;;;;;;;;;;;;;;;;;;

(defn orders-at [w order-type floor]
  (get-in w
          (case order-type
            :call [:calls floor 0]
            :goto [:gotos floor])))

(defn orders-from [w order-type up? from]
  "sums the count of orders starting at from in the direction indicated by up?"
  (let [iter (if up? inc dec)
        limit (if up? num-floors -1)]
    (loop [floor from
           total 0]
      (if (= floor limit)
        total
        (recur (iter floor) (+ total (orders-at w order-type floor)))))))

(defn all-orders-from [w up?]
  (let [from (w :floor)
        calls (orders-from w :call up? from)
        gotos (orders-from w :goto up? from)]
    (+ calls gotos)))

(defn count-orders [w]
  (let [calls (apply + (map #(% 0) (w :calls)))
        gotos (apply + (w :gotos))]
    (+ calls gotos)))

(defn next-order [w order-type up? from]
  "finds the floor of the next order after the floor from"
  (let [iter (if up? inc dec)
        limit (if up? (dec num-floors) 0)]
    (loop [floor from]
      (cond
       (pos? (orders-at w order-type floor)) floor
       (= limit floor) nil
       :else (recur (iter floor))))))

(defn next-stop [w up? from]
  (let [nxt-call (next-order w :call up? from)
        nxt-goto (next-order w :goto up? from)]
    (cond
     (and (nil? nxt-call) (nil? nxt-goto)) (int (/ num-floors 2))
     (nil? nxt-call) nxt-goto
     (nil? nxt-goto) nxt-call
     :else ((if up? min max) nxt-call nxt-goto))))

;; If no orders then go to middle
;; If at top or bottom or no orders in current direction, reverse to next
;; Else goto next in current direction
(defn next-move [w up?]
  (let [floor (w :floor)]
    (if (zero? (count-orders w))
      (let [middle (int (/ num-floors 2))
            next-up? (< floor middle)]
        [middle next-up?])
      (if (or
           (and (zero? floor) (not up?))
           (and (= floor (dec num-floors)) up?)
           (zero? (all-orders-from w up?)))
        [(next-stop w (not up?) floor) (not up?)]
        [(next-stop w up? floor) up?]))))

(defn iter-plan [plan]
  (let [path (plan :path)]
    (if (> (count path) 1)
      (assoc plan :path (rest path))
      plan)))

;; assumes only called with :calls or :gotos change
;; if 1 waypoint left, leave it there, else remove head to procede with plan
;; start replanning
(defn plan-standard [wc best-plan]
  (loop [w (async/<!! wc)  ;; exits when channel is closed
         up? true]
    (try
      (if w
        (do
          (swap! best-plan iter-plan)
          (let [floor (w :floor)
                [waypoint nxt-up?] (next-move w up?)]
            (swap! best-plan assoc :path [waypoint] :world w)
            (recur (async/<!! wc) nxt-up?)))
        (println "Channel closed.  Exiting Planner.")))
    (catch Exception e
      (do
        (shutdown)
        (println (.toString e "\n\nworld=" w "\\nbest-plan=" @best-plan))))))


(defn- current-waypoint [plan]
  (first (plan :path)))

(defn- default-plan [config]
  {:path [(int (/ (config :num-floors) 2))] :cost 0 :world {}})

;;;;;;;;;;;;;;;;;;;;;
;; Public API
;;;;;;;;;;;;;;;;;;;;;

(defn init [config world-chan]
  "Inits and runs planning.  Returns a function that returns
  the best plan at that moment in time."
  (let [best-plan (atom (default-plan config))
        planning (future (plan-standard world-chan best-plan))]
    (fn [] (current-waypoint @best-plan))))
