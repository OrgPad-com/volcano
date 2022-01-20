(ns volcano.hiccup
  (:require [clojure.string :as str]))

(defn- expand-resources-inner
  "Returns a sequence of hiccups in which all resource keys are replaced with resource values."
  [resources hiccup]
  (cond (seq? hiccup) (->> hiccup (map (partial expand-resources-inner resources))
                           (remove nil?)
                           (apply concat))
        (contains? resources hiccup) (->> hiccup (get resources)
                                          (map (partial expand-resources-inner resources))
                                          (remove nil?)
                                          (apply concat))
        (not (vector? hiccup)) [hiccup]
        :else (let [[tag & children] hiccup]
                [(into [tag] (->> children (map (partial expand-resources-inner resources))
                                  (apply concat) (remove nil?)))])))

(defn expand-resources
  "Replaces reagent style maps with style strings, expected by hiccup."
  [resources hiccup]
  (first (expand-resources-inner resources hiccup)))

(defn- replace-path
  "Either adds a path prefix to path, or converts path to a relative path from page-route."
  [path page-route {:keys [relative-paths path-prefix]}]
  (if relative-paths
    (let [path-vector (str/split path #"/")
          route-vector (str/split page-route #"/")
          common-dirs (second (reduce (fn [[common num] [path-dir route-dir]]
                                        (if (and common (= path-dir route-dir))
                                          [true (inc num)]
                                          [false num]))
                                      [true 0] (map vector path-vector route-vector)))]
      (str (apply str (repeat (- (count route-vector) (inc common-dirs)) "../"))
           (str/join "/" (drop common-dirs path-vector))))
    (str path-prefix (subs path 1))))

(defn- replace-href-and-src
  "Replaces href and src paths starting with /."
  [page-route config {:keys [href src] :as attributes}]
  (cond-> attributes (and (string? href) (str/starts-with? href "/")) (update :href replace-path page-route config)
          (and (string? src) (str/starts-with? src "/")) (update :src replace-path page-route config)))

(defn replace-paths
  "Replaces href and src paths starting with / for all tags."
  [page-route {:keys [relative-paths path-prefix] :as config} hiccup]
  (cond (not (or relative-paths path-prefix)) hiccup
        (seq? hiccup) (map replace-paths hiccup)
        (not (vector? hiccup)) hiccup
        :else (let [[tag maybe-attributes & children] hiccup]
                (if (not (map? maybe-attributes))
                  (->> children (into [maybe-attributes])
                       (remove nil?) (map (partial replace-paths page-route config)) (into [tag]))
                  (into [tag (replace-href-and-src page-route config maybe-attributes)]
                        (map (partial replace-paths page-route config) children))))))