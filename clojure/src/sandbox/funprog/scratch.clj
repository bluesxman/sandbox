(def coins [5,10,20,50,100,200,500])
(range (/ 300 5))

(defn all-but [b all]
  (remove #(= % b) all))

(defn count-change
  [money coins]
  (letfn [(cchange [m c cs]
                   (if (zero? m)
                     1
                     (if (zero? (mod m c))
                       (+ (cchange (- m c) c cs)
                          (reduce + (map #(cchange (- m c) % (all-but % cs)) cs)))
                       0)))]
    (cchange money (first coins) (rest coins))))

;; (apply + (reduce recur args))

(count-change 300 coins)

(count-change 4 [1 2])

(count-change 300 [100 200 500])

(all-but 10 coins)

(map #([(- 300 (* % 5)) (rest coins)]) (range (/ 300 5)))

// if divides exactly once then 1 way
// else if divides 0 then 0 way
// else then sum the recurse of n ways with the current coin removed from the coin list
//      where n is the number of times the current coin divides into money


60 5s
58 5s 1 10s
56 5s 2 10s
56 5s 1 20s
54 5s 3 10s
54 5s 1 10s 1 20s
52 5s 4 10s
52 5s 2 10s 1 20s
52 5s 2 20s
