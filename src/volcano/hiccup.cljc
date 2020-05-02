(ns volcano.hiccup
  (:require [clojure.string :as str]))

(defn- style->str
  "Replaces a style map with a style string."
  [{:keys [style] :as attributes}]
  (if (map? style)
    (assoc attributes :style (->> style (map (fn [[style-key style-value]]
                                               (str (name style-key) ":" (if (number? style-value)
                                                                           (str style-value "px")
                                                                           style-value))))
                                  (str/join ";")))
    attributes))

(defn expand-styles
  "Replaces reagent style maps with style strings, expected by Hiccup library."
  [hiccup]
  (cond (seq? hiccup) (map expand-styles hiccup)
        (not (vector? hiccup)) hiccup
        :else (let [[tag maybe-attributes & children] hiccup]
                (if (not (map? maybe-attributes))
                  (->> children (into [maybe-attributes])
                       (map expand-styles) (into [tag]))
                  (into [tag (style->str maybe-attributes)]
                        (map expand-styles children))))))

(defn- expand-resources-inner
  "Returns a sequence of hiccups in which all resource keys are replaced with resource values."
  [resources hiccup]
  (cond (seq? hiccup) (->> hiccup (map (partial expand-resources-inner resources))
                           (apply concat))
        (contains? resources hiccup) (->> hiccup (get resources)
                                          (map (partial expand-resources-inner resources))
                                          (apply concat))
        (not (vector? hiccup)) [hiccup]
        :else (let [[tag & children] hiccup]
                [(into [tag] (apply concat (map (partial expand-resources-inner resources) children)))])))

(defn expand-resources
  "Replaces reagent style maps with style strings, expected by hiccup."
  [resources hiccup]
  (first (expand-resources-inner resources hiccup)))