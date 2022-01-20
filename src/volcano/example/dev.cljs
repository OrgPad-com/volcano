(ns volcano.example.dev
  (:require [volcano.dev :as volcano]
            [volcano.example.config :as config]))

(defn mount-root
  "Rendering of the current page inside :div#app element."
  []
  (volcano/mount-root (config/config)))

(defn init
  "Init function of the dev."
  []
  (volcano/set-routing! (config/config))
  (mount-root))