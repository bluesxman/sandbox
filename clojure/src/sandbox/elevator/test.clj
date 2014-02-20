(ns sandbox.elevator.test
  (:require [sandbox.elevator.dropship :refer :all]))

(call "atFloor=5&to=DOWN")
(vector 1 :up)
(async/<!! world-chan)
(go "floorToGo=1")
(async/close! world-chan)

(swap! world assoc :door :open :floor 2)
(enter)
(swap! world assoc :floor 1)
(exit)


;;;;;;;;;

(defn assert-path [waypoint]
  (assert (= waypoint (get-in @best-path [:path 0]))))

(defn assert-next-cmd [cmd waypoint floor]
  (assert (= cmd (next-cmd)))
  (assert-path waypoint)
  (assert (= floor (@world :floor))))

(defn assert-world [floor call-flr call-cnt goto-flr goto-cnt]
  (assert (= floor (@world :floor)))
  (assert (= call-cnt (get-in @world [:calls call-flr 0])))
  (assert (= goto-cnt (get-in @world [:gotos goto-flr]))))


(get-in @best-path [:path 0])
(<!! world-chan)

(reset "")

(assert-next-cmd up-cmd 3 1)
(call "atFloor=0&to=UP")
(assert-world 1 0 1 0 0)
(assert-path 0)
(assert-next-cmd down-cmd 0 0)
(assert-next-cmd open-cmd 0 0)

;;;;;;;
(eval @best-plan)
(eval @world)
(next-cmd)

