(call "atFloor=5&to=DOWN")
(vector 1 :up)
(async/<!! world-chan)
(go "floorToGo=1")
(async/close! world-chan)

(swap! world assoc :door :open :floor 2)
(enter)
(swap! world assoc :floor 1)
(exit)
