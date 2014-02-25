(ns sandbox.elevator.dropship
  (:require [sandbox.elevator.handling :as handling]
            [sandbox.elevator.planning :as planning]
            [clojure.core.async :as async]))

;;;;;;;;;;;;;;;;;;;;;
;; Functions
;;;;;;;;;;;;;;;;;;;;;

(defn init [config]
  "Creates, intializes, and runs an elevator server.
  Returns a function for shutting down the server."
  (let [world-chan (async/chan)
        get-best-plan (planning/init config world-chan)
        shutdown-handling (handling/init config world-chan get-best-plan)]
    (fn []
      (do
        (shutdown-handling)
        (async/close! world-chan)))))


;;;;;;;;;;;;;;;;;;;;;
;; Configuration
;;;;;;;;;;;;;;;;;;;;;

(def config
  {:num-floors 6
   :num-plan-threads 4
   :listen-port 9090})


;;;;;;;;;;;;;;;;;;;;;
;; Execution
;;;;;;;;;;;;;;;;;;;;;

(def shutdown (init config))
(shutdown)
