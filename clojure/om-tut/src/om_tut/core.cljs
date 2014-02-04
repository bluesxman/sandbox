(ns om-tut.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def app-state (atom {:list ["Lion" "Zebra" "Buffalo" "Antelope"]}))

(def app-state (atom {:list ["Lion" "Zebra" "Buffalo" "Dog"]}))

(om/root
  app-state
  (fn [app owner]
    (apply dom/ul nil
      (map (fn [text] (dom/li nil text)) (:list app))))
  (. js/document (getElementById "app0")))

(om/root
  app-state
  (fn [app owner]
    (apply dom/ul #js {:className "animals"}
      (map (fn [text] (dom/li nil text)) (:list app))))
  (. js/document (getElementById "app0")))

(om/root
  app-state
  (fn [app owner]
    (dom/h2 nil (:text app)))
  (. js/document (getElementById "app1")))

(swap! app-state assoc :text "Multiple r!")

(swap! app-state assoc :list ["Lion" "Zebra" "booj" "Cat"])
