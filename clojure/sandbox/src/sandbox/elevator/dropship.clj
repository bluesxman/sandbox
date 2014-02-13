;; Copied from Laurent Petit's omnibus example:
;; http://laurentpetit.github.io/blog/2013/11/05/the-clojure-omnibus/

(ns sandbox.elevator.dropship
  (:require [ring.adapter.jetty :refer :all]
            [compojure.core :refer :all]))

(defn make-omnibus [nb-floors]
  (let [up   (repeat (dec nb-floors) ["OPEN", "CLOSE", "UP"])
        down (repeat (dec nb-floors) ["OPEN", "CLOSE", "DOWN"])
        up-then-down (flatten (concat up down))]
    (concat ["NOTHING"] (cycle up-then-down))))

(defn tick [elevator] (rest elevator))
(defn next-command [elevator] (first elevator))

(def nb-floors 20)
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
  (GET "/call?atFloor=:floor&to=:dir" [floor dir] (println "call on " floor " going " dir))
  (GET "/go?floorToGo=:floor" [floor] (println "goto floor " floor))
;;   (GET "/userHasEntered" [] (println "user has entered"))
;;   (GET "/userHasExited" [] (println "user has exited"))
;;   (GET "/reset?cause=:info" [info] (println "info+msg: " info))
;;   (GET "/reset" [] (println "reseting..."))
  (GET "/:unmatched:nxt" [unmatched nxt] (println "unknown request: " unmatched nxt))
;;   (GET "*" [] (println "unmatched")))
  (GET "*" [] ""))

(def requests (atom []))
(defn print-requests
  [req]
  (println req))
(defn store-requests
  [req]
  (swap! requests conj req))

(def server (run-jetty app {:port 9090 :join? false}))
(def server (run-jetty print-requests {:port 9090 :join? false}))
(def server (run-jetty store-requests {:port 9090 :join? false}))

(.stop server)
;; (.start server)

;; (println server)

(swap! requests conj {:foo 1})
(println @requests)

(count @requests)
(@requests 2)
