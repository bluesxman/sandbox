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

;;;;;;;;;;;;;;;;;;;;;;;;;

;; when arrive, gotos = 0, add calls to gotos, calls = 0
;; gotos = calls, calls = 0


2014-02-20 17:02:24.032     HTTPElevator http://localhost:9090/reset?cause=the+elevator+is+at+the+lowest+level+and+its+doors+are+closed
2014-02-20 17:02:24.353     HTTPElevator http://localhost:9090/call?atFloor=1&to=UP
2014-02-20 17:02:24.360     HTTPElevator http://localhost:9090/nextCommand UP
2014-02-20 17:02:25.353     HTTPElevator http://localhost:9090/call?atFloor=0&to=UP
2014-02-20 17:02:25.369     HTTPElevator http://localhost:9090/nextCommand

;;;;

2014-02-20 16:36:53.590     HTTPElevator http://localhost:9090/reset?cause=the+e
levator+is+at+the+lowest+level+and+its+doors+are+closed
2014-02-20 16:36:54.353     HTTPElevator http://localhost:9090/call?atFloor=4&to
=UP
2014-02-20 16:36:54.368     HTTPElevator http://localhost:9090/nextCommand UP
2014-02-20 16:36:55.353     HTTPElevator http://localhost:9090/call?atFloor=0&to
=UP
2014-02-20 16:36:55.369     HTTPElevator http://localhost:9090/nextCommand UP
2014-02-20 16:36:56.353     HTTPElevator http://localhost:9090/call?atFloor=2&to
=DOWN
2014-02-20 16:36:56.368     HTTPElevator http://localhost:9090/nextCommand UP
2014-02-20 16:36:57.359     HTTPElevator http://localhost:9090/nextCommand UP
2014-02-20 16:36:58.357     HTTPElevator http://localhost:9090/nextCommand OPEN
2014-02-20 16:36:58.365     HTTPElevator http://localhost:9090/userHasEntered
2014-02-20 16:36:58.369     HTTPElevator http://localhost:9090/go?floorToGo=5
2014-02-20 16:36:59.358     HTTPElevator http://localhost:9090/nextCommand CLOSE

2014-02-20 16:37:00.357     HTTPElevator http://localhost:9090/nextCommand DOWN
2014-02-20 16:37:01.356     HTTPElevator http://localhost:9090/nextCommand DOWN
2014-02-20 16:37:02.358     HTTPElevator http://localhost:9090/nextCommand OPEN
2014-02-20 16:37:02.366     HTTPElevator http://localhost:9090/userHasEntered
2014-02-20 16:37:02.383     HTTPElevator http://localhost:9090/go?floorToGo=0
2014-02-20 16:37:03.356     HTTPElevator http://localhost:9090/nextCommand CLOSE

2014-02-20 16:37:04.354     HTTPElevator http://localhost:9090/nextCommand DOWN
2014-02-20 16:37:05.359     HTTPElevator http://localhost:9090/nextCommand DOWN
2014-02-20 16:37:06.355     HTTPElevator http://localhost:9090/nextCommand OPEN
2014-02-20 16:37:06.357     HTTPElevator http://localhost:9090/userHasEntered
2014-02-20 16:37:06.372     HTTPElevator http://localhost:9090/go?floorToGo=2
2014-02-20 16:37:06.380     HTTPElevator http://localhost:9090/userHasExited
2014-02-20 16:37:07.352     HTTPElevator http://localhost:9090/call?atFloor=4&to
=UP
2014-02-20 16:37:07.373     HTTPElevator http://localhost:9090/nextCommand CLOSE

2014-02-20 16:37:08.356     HTTPElevator http://localhost:9090/nextCommand UP
2014-02-20 16:37:09.359     HTTPElevator http://localhost:9090/nextCommand UP
2014-02-20 16:37:10.357     HTTPElevator http://localhost:9090/nextCommand OPEN
2014-02-20 16:37:10.367     HTTPElevator http://localhost:9090/userHasExited
2014-02-20 16:37:11.353     HTTPElevator http://localhost:9090/call?atFloor=0&to
=UP
2014-02-20 16:37:11.359     HTTPElevator http://localhost:9090/nextCommand CLOSE

2014-02-20 16:37:12.355     HTTPElevator http://localhost:9090/nextCommand OPEN
2014-02-20 16:37:13.354     HTTPElevator http://localhost:9090/nextCommand CLOSE

2014-02-20 16:37:14.355     HTTPElevator http://localhost:9090/nextCommand OPEN
2014-02-20 16:37:15.356     HTTPElevator http://localhost:9090/nextCommand CLOSE

2014-02-20 16:37:16.358     HTTPElevator http://localhost:9090/nextCommand OPEN
2014-02-20 16:37:17.357     HTTPElevator http://localhost:9090/nextCommand CLOSE

2014-02-20 16:37:18.360     HTTPElevator http://localhost:9090/nextCommand OPEN
2014-02-20 16:37:19.357     HTTPElevator http://localhost:9090/nextCommand CLOSE




