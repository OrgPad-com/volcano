(ns volcano.example.dev
  (:require [volcano.dev :as volcano]
            [reagent.dom :as r-dom]
            [volcano.example.config :as config]))

(defn mount-root
  "Rendering of the current page inside :div#app element."
  []
  (volcano/load-scripts! (config/config))
  (r-dom/render [volcano/render (config/config)]
                (.getElementById js/document "app")))

(defn init
  "Init function of the dev."
  []
  (volcano/set-routing! (config/config))
  (mount-root))