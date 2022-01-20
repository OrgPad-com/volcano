(ns volcano.example.build
  (:require [volcano.build :as build]
            [volcano.example.config :as config]))

(defn build
  []
  (build/build-web! (config/config))
  (.exit js/process))