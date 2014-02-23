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


;; TODO: using a tuple for the elements in calls is very awkward
;; better to split calls into multiple vectors (e.g. :calls, :ups, :downs)
(def start-world
  {:calls (vec (repeat num-floors [0 :none])) ;; WARN: we're only recording the last direction!
   :gotos (vec (repeat num-floors 0))
   :floor 0
   :door :closed})
(def world (atom start-world))
(def world-chan (async/chan))

(def default-plan {:path [(int (/ num-floors 2))] :cost 0 :world start-world})
(def best-plan (atom default-plan))

(def open-cmd "OPEN")
(def close-cmd "CLOSE")
(def up-cmd "UP")
(def down-cmd "DOWN")
(def nothing-cmd "NOTHING")

;;;;;;;;;;;;;;;;;;;;;
;; Functions
;;;;;;;;;;;;;;;;;;;;;

(defn notify-change [w]
  (async/thread (async/>!! world-chan w)))

;; next-cmd helpers ;;;;;;;;;;

(defn door-open? [w]
  (= (w :door) :open))

(defn exiting? [w]
  (pos? (get-in w [:gotos (w :floor)])))

(defn leave []
  (swap! world assoc :door :closed)
  close-cmd)

(defn at-waypoint? [w goal]
  (= (w :floor) goal))

;; WARN: should use the enter exit events or not?  website says all exit at once...
;; if not then arrive should also change :gotos
(defn arrive []
  (letfn [(update [w]
                  (let [floor (w :floor)
                        gotos (assoc (w :gotos) floor 0)
                        calls (assoc-in (w :calls) [floor 0] 0)]
                    (assoc w
                      :door :open
                      :calls calls
                      :gotos gotos)))]
    (notify-change (swap! world update))
    open-cmd))

(defn travel [w goal]
  (let [floor (w :floor)]
    (if (= goal floor)
      nothing-cmd ;; should not happen.  arrive should have been called
      (let [[iter cmd] (if (> goal floor) [inc up-cmd] [dec down-cmd])]
        (swap! world update-in [:floor] iter)
        cmd))))

;; request handler targets ;;;;;;;;;;

(defn next-cmd []
  (let [w @world          ;; Take a snapshot of the world and reason on it
        goal (first (@best-plan :path))]     ;; we know p was made for this world
    (if (door-open? w)
      (leave)
      (if (at-waypoint? w goal)
        (arrive)
        (travel w goal)))))

(eval @world)
(eval @best-plan)
(next-cmd)
(door-open? @world)
(first (@best-plan :path))
(at-waypoint? @world 0)
(arrive)

(defn reset [qs]
  (notify-change (reset! world start-world))
  (println "reseting: " qs))

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

;; (defn enter []
;;   (swap! world #(update-in % [:calls (% :floor) 0] dec)))

;; (defn exit []
;;   (swap! world #(update-in % [:gotos (% :floor)] dec)))

(defroutes handle-requests
  (GET "/nextCommand" [] (next-cmd))
  (GET "/call" {qs :query-string} (call qs) "")
  (GET "/go" {qs :query-string} (go qs) "")
  (GET "/userHasEntered" [] "")
  (GET "/userHasExited" [] "")
  (GET "/reset" {qs :query-string} (reset qs) ""))

;; planning ;;;;;;;;;;

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

(orders-from @world :call true 0)

(get-in @world [:calls 0 0])

(defn all-orders-from [w up?]
  (let [from (w :floor)
        calls (orders-from w :call up? from)
        gotos (orders-from w :goto up? from)]
    (+ calls gotos)))

(defn count-orders [w]
  (let [calls (apply + (map #(% 0) (w :calls)))
        gotos (apply + (w :gotos))]
    (+ calls gotos)))

(count-orders @world)

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

(next-move p-world false)
(eval p-world)
(count-orders @world)
(next-stop p-world true 0)
(all-orders-from @world true)


(defn iter-plan [plan]
  (let [path (plan :path)]
    (if (> (count path) 1)
      (assoc plan :path (rest path))
      plan)))

;; assumes only called with :calls or :gotos change
;; if 1 waypoint left, leave it there, else remove head to procede with plan
;; start replanning
(defn plan-standard [wc]
  (try
    (loop [w (async/<!! wc)  ;; exits when channel is closed
           up? true]
      (println "got world")
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
        (println (.toString e "\n\nworld=" @world "\\nbest-plan=" @best-plan))))))

(println @world)
(eval @best-plan)
(eval @world)
(next-cmd)

(async/>!! world-chan @world)

(next-move @world true)
(next-move @world false)

(all-orders-from @world false)

;;;;;;;;;;;;;; Testing
(defn shutdown []
  (.stop server)
  (async/close! world-chan))

(shutdown)
(def world-chan (async/chan))
(def planner (future (plan-standard world-chan)))
(reset "")

(def server (run-jetty handle-requests {:port 9090 :join? false}))

(eval @world)
(eval @best-plan)
(async/>!! world-chan @world)

(def p-world (@best-plan :world))
(next-move p-world false)
(eval p-world)

(next-move @world false)

(call "call?atFloor=1&to=UP")
(next-cmd)
(call "call?atFloor=0&to=UP")
(next-cmd)
(next-cmd)
(next-cmd)

(async/<!! world-chan)

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
