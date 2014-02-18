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
;; Data
;;;;;;;;;;;;;;;;;;;;;

(def default-path {:path [(int (/ num-floors 2))] :cost 0})
(def best-path (ref default-path))


;; TODO: using a tuple for the elements in calls is very awkward
;; better to split calls into multiple vectors (e.g. :calls, :ups, :downs)
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

(defn notify-change [w]
  (async/thread (async/>!! world-chan w)))

;; next-cmd helpers ;;;;;;;;;;

(defn update-world [f & args]
  (async/thread (async/>!! world-chan (swap! world f args))))

(defn door-open? [w]
  (= (w :door) :open))

(defn exiting? [w]
  (pos? (get-in w [:gotos (w :floor)])))

(defn leave []
  (swap! world assoc :door :closed)
  close-cmd)

(defn at-waypoint? [w p]
  (= (w :floor) (first p)))

(defn arrive []
  (dosync
   (let [new-path (rest (@best-path :path))]
     (alter best-path assoc :path new-path)))
  (swap! world assoc :door :open)
  open-cmd)

(defn travel [w]
  (dosync
   (let [goal (first (@best-path :path))]
     (if (> goal (w :floor))
       (do
         (swap! world update-in [:floor] inc)
         up-cmd)
       (do
         (swap! world update-in [:floor] dec)
         down-cmd)))))

(travel @world)

;; request handler targets ;;;;;;;;;;

(defn next-cmd []
  (let [w @world          ;; Take a snapshot of the world and reason on it
        p (@best-path :path)]     ;; we know p was made for this world
    (if (door-open? w)
      (if (exiting? w)
        open-cmd ;; just repeat open until all passengers exit
        (leave)) ;; WARN: not handling special case of no calls AND no gotos
      (if (at-waypoint? w p)
        (arrive)
        (travel w)))))

(next-cmd)
(eval @world)
(door-open? @world)
(@best-path :path)
(at-waypoint? @world (@best-path :path))
(exiting? @world)
(leave)

(call "atFloor=0&to=UP")
(go "floorToGo=3")

(defn reset [qs]
  (reset! world start-world)
  (dosync (ref-set best-path default-path)))

(defn call [qs]
  (let [[_ fs ds] (re-find #"atFloor=(\d+)&to=(\S+)" qs)
        floor (read-string fs)
        dir (if (= "UP" ds) :up :down)
        update (fn [[cnt d]] (vector (inc cnt) dir))]
    (notify-change (swap! world #(update-in % [:calls floor] update)))))

(defn go [qs]
  (let [[_ floor-str] (re-find #"floorToGo=(\d+)" qs)
        floor (read-string floor-str)]
    (notify-change (swap! world #(update-in % [:gotos floor] inc)))))

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

;; planning ;;;;;;;;;;

(defn orders-at [w order-type floor]
  (get-in w
          (case order-type
            :call [:calls floor 0]
            :goto [:gotos floor])))

(defn orders-from [w order-type up? from]
  "sums the count of orders after from in the direction indicated by up?"
  (let [iter (if up? inc dec)
        limit (if up? (dec num-floors) 0)]
    (loop [floor (iter from)
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

(eval @world)
(count-orders @world)

(defn next-order [w order-type up? from]
  "finds the floor of the next order after the floor from"
  (let [iter (if up? inc dec)
        limit (if up? (dec num-floors) 0)]
    (loop [floor (iter from)]
      (cond
       (pos? (orders-at w order-type floor)) floor
       (= limit floor) nil
       :else (recur (iter floor))))))

(defn next-stop [w up? from]
  (let [nxt-call (next-order w :call up? from)
        nxt-goto (next-order w :goto up? from)]
    (cond
     (and (nil? nxt-call) (nil? nxt-goto)) nil
     (nil? nxt-call) nxt-goto
     (nil? nxt-goto) nxt-call
     :else ((if up? min max) nxt-call nxt-goto))))

;; assumes planning only needed when go or call events occur
(defn plan-simple [wc]
  (loop [w (async/<!! wc)]  ;; exits when channel is closed
    (if w
      (let [floor (w :floor)
            above (+ (map #(orders-from w % true floor) [:call :goto]))
            below (+ (map #(orders-from w % false floor) [:call :goto]))
            up? (> above below)]
        (next-stop w up? floor)
        (recur (async/<!! wc))))))

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
           (zero? floor)
           (= floor (dec num-floors))
           (zero? (all-orders-from w up?)))
        [(next-stop w (not up?) floor) (not up?)]
        [(next-stop w up? floor) up?]))))

(defn plan-standard [wc]
  (loop [w (async/<!! wc)  ;; exits when channel is closed
         up? true]
    (if w
      (let [floor (w :floor)
            [waypoint nxt-up?] (next-move w up?)]
        (dosync (alter best-path assoc :path [waypoint]))
        (recur (async/<!! wc) nxt-up?)))))


;;;;;;;;;;;;;; Testing
(defn restart []
  (.stop server)
  (async/close! world-chan)
  (reset ""))

(restart)
(def world-chan (async/chan))

(def server (run-jetty handle-requests {:port 9090 :join? false}))
(def planner (future (plan-standard world-chan)))

(eval @world)

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

;; (defn submit-world
;;   [w tp]
;;   ())

;; (defn plan [wc]
;;   (loop [tp (Executors/newFixedThreadPool num-plan-threads)
;;          w (async/<!! wc)]
;;     (if w
;;       (recur (submit-world w tp) (async/<!! wc)))))
