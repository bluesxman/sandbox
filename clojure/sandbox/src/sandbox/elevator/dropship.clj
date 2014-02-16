(ns sandbox.elevator.dropship
  (:require [ring.adapter.jetty :refer :all]
            [compojure.core :refer :all]
            [clojure.core.async :as async]))

(def event-queue (async/chan (async/sliding-buffer 50)))

(def request-chan (async/chan (async/sliding-buffer 50)))

(defn new-world [n-floors]
  {:calls (vec (repeat n-floors 0))
   :gotos (vec (repeat n-floors 0))
   :in-car 0
   :floor 0
   :door :closed})

(defn start-world (new-world 6))

(def world (atom start-world))

;; planning needs to read and alter the commands.  handler need to remove and provide next-cmd
;; best data structure????  Try queue for now
(def commands (ref (clojure.lang.PersistentQueue/EMPTY)))

(defn next-cmd []
  (dosync
   (let [head (peek @commands)]
     (case head
       "OPEN" (swap! world assoc :door :open)
       "CLOSE" (swap! world assoc :door :closed)
       "UP" (swap! world (fn [w] (assoc w :floor (inc (w :floor)))))
       "DOWN" (swap! world (fn [w] (assoc w :floor (dec (w :floor))))))
     (alter commands pop)
     head)))

(defn reset [qs]
  (dosync
   (ref-set commands clojure.lang.PersistentQueue/EMPTY)
   (reset! world start-world))
  "elevator reset")

(defn queue-event [e] (async/thread (async/>!! event-queue e)) (str e))

(defn queue-call [qs]
  (let [[_ floor dir] (re-find #"atFloor=(\d+)&to=(\S+)" qs)]
    (queue-event [:call (read-string floor) (if (= "UP") :up :down)])))

(defn queue-go [qs]
  (let [[_ floor] (re-find #"floorToGo=(\d+)" qs)]
    (queue-event [:go (read-string floor)])))

(defn queue-entered [] (queue-event [:enter]))

(defn queue-exited [] (queue-event [:exit]))

(defroutes route
  (GET "/nextCommand" [] (next-cmd))
  (GET "/call" {qs :query-string} (queue-call qs))
  (GET "/go" {qs :query-string} (queue-go qs))
  (GET "/userHasEntered" [] (queue-entered))
  (GET "/userHasExited" [] (queue-exited))
  (GET "/reset" {qs :query-string} (reset qs)))

(defn queue-requests [request]
  (async/thread (async/>!! request-chan request)))

(defn process-requests [num-threads req-chan]
  (dotimes [n num-threads]
    (async/thread
     (loop [r (async/<!! req-chan)]
       (if r
         (do
           (route r)
           (recur (async/<!! req-chan))))))))

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
(defn plan [] ())

;;;;;;;;;;;;;; Testing

(dosync (dotimes [n 10] (alter commands conj "OPEN" "CLOSE" "UP" "OPEN" "CLOSE" "DOWN")))

(def server (run-jetty handle-requests {:port 9090 :join? false}))

(.stop server)

(async/close! event-queue)
(def events
  (loop [events []]
    (let [e (async/<!! event-queue)]
      (if e
        (recur (conj events e))
        events))))
(eval events)



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
