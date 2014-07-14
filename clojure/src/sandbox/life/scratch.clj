
(def balance (atom 100))
(eval @balance)

(swap! balance (fn [b] (- b 20)))
(def eighty @balance)

(swap! balance #(- % 20))
(swap! balance - 10 10)

(eval @balance)
(eval eighty)
