(ns sandbox.data.tree)

(def tree
  {:root {:val 1
          :a {:val 2
              :c {:val 4}}
          :b {:val 3}}})

(defn child-keys
  [tree path]
  (remove #(= % :val) (keys (get-in tree path))))

(defn node-in
  [tree path]
  (hash-map (peek path) (get-in tree path)))

(defn val-in
  [tree path]
  (:val (get-in tree path)))

(defn children
  [tree path]
  (dissoc (get-in tree path) :val))

(defn assoc-val
  [tree path v]
  (assoc (get-in tree path) :val v))

(defn assoc-node
  []
  ())

(child-keys tree [:root])

(val-in tree [:root :a :c])

(children tree [:root :a])

(node-in tree [:root :a])

(assoc-val tree [:root :a] 9)


