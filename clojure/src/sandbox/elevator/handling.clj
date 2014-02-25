(ns sandbox.elevator.handling
  (:require [ring.adapter.jetty :refer :all]
            [compojure.core :refer :all]
            [clojure.core.async :as async]))


;;;;;;;;;;;;;;;;;;;;;
;; Constants
;;;;;;;;;;;;;;;;;;;;;

(def open-cmd "OPEN")
(def close-cmd "CLOSE")
(def up-cmd "UP")
(def down-cmd "DOWN")
(def nothing-cmd "NOTHING")


;;;;;;;;;;;;;;;;;;;;;
;; Data
;;;;;;;;;;;;;;;;;;;;;

;; TODO: using a tuple for the elements in calls is very awkward
;; better to split calls into multiple vectors (e.g. :calls, :ups, :downs)
(def- start-world
  {:calls (vec (repeat num-floors [0 :none])) ;; WARN: we're only recording the last direction!
   :gotos (vec (repeat num-floors 0))
   :floor 0
   :door :closed})
(def- world (atom start-world))

;;;;;;;;;;;;;;;;;;;;;
;; Functions
;;;;;;;;;;;;;;;;;;;;;

(defn- notify-change [w]
  (async/thread (async/>!! world-chan w)))

;; next-cmd helpers ;;;;;;;;;;

(defn- door-open? [w]
  (= (w :door) :open))

(defn- exiting? [w]
  (pos? (get-in w [:gotos (w :floor)])))

(defn- leave []
  (swap! world assoc :door :closed)
  close-cmd)

(defn- at-waypoint? [w goal]
  (= (w :floor) goal))

;; WARN: should use the enter exit events or not?  website says all exit at once...
;; if not then arrive should also change :gotos
(defn- arrive []
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

(defn- travel [w goal]
  (let [floor (w :floor)]
    (if (= goal floor)
      nothing-cmd ;; should not happen.  arrive should have been called
      (let [[iter cmd] (if (> goal floor) [inc up-cmd] [dec down-cmd])]
        (swap! world update-in [:floor] iter)
        cmd))))

;; request handler targets ;;;;;;;;;;

(defn- next-cmd []
  (let [w @world          ;; Take a snapshot of the world and reason on it
        goal (first (@best-plan :path))]     ;; we know p was made for this world
    (if (door-open? w)
      (leave)
      (if (at-waypoint? w goal)
        (arrive)
        (travel w goal)))))

(defn- reset [qs]
  (notify-change (reset! world start-world))
  (println "reseting: " qs))

(defn- call [qs]
  (let [[_ fs ds] (re-find #"atFloor=(\d+)&to=(\S+)" qs)
        floor (read-string fs)
        dir (if (= "UP" ds) :up :down)
        update (fn [[cnt d]] (vector (inc cnt) dir))]
    (notify-change (swap! world #(update-in % [:calls floor] update)))))

(defn- go [qs]
  (let [[_ floor-str] (re-find #"floorToGo=(\d+)" qs)
        floor (read-string floor-str)]
    (notify-change (swap! world #(update-in % [:gotos floor] inc)))))

(defroutes handle-requests
  (GET "/nextCommand" [] (next-cmd))
  (GET "/call" {qs :query-string} (call qs) "")
  (GET "/go" {qs :query-string} (go qs) "")
  (GET "/userHasEntered" [] "")
  (GET "/userHasExited" [] "")
  (GET "/reset" {qs :query-string} (reset qs) ""))


;;;;;;;;;;;;;;;;;;;;;
;; Public API
;;;;;;;;;;;;;;;;;;;;;

(defn init [config]
  (let [port (config :listen-port)
        server (run-jetty handle-requests {:port port :join? false})]
    (fn [] (.stop server))))

