(ns volcano.build
  (:require [me.raynes.fs :as fs]
            [hiccup.core :as hiccup]
            [volcano.hiccup :as volcano-hiccup]
            [bidi.bidi :as b]
            [clojure.string :as str]))

(defn- copy-resources!
  "Copies all files from resources/ except index.html and js directory into build/."
  [{:keys [resource-dir target-dir exclude-files exclude-dirs]}]
  (println "Copying static resources to build/ ...")
  (fs/delete-dir target-dir)
  (fs/copy-dir resource-dir target-dir)
  (doseq [file exclude-files]
    (fs/delete (str target-dir "/" file)))
  (doseq [dir exclude-dirs]
    (fs/delete-dir (str target-dir "/" dir))))

(defn- generate-hiccup
  "Generates a hiccup from the given template with inserted hiccups, expanded resources and styles."
  [hiccups template {:keys [resources]}]
  (->> template (volcano-hiccup/expand-resources (assoc resources :volcano/hiccups hiccups))
      volcano-hiccup/expand-styles))

(defn- build-site!
  "Build a single site-key of the given keyword and hiccup."
  [site-key {:keys [sites routes default-template target-dir] :as config}]
  (println "Building site" site-key "...")
  (let [{:keys [hiccups template] :or {template default-template}} (get sites site-key)
        route (b/path-for routes site-key)
        path (str target-dir route)
        content (hiccup/html (generate-hiccup hiccups template config))]
    (when-let [last-index (str/last-index-of (subs route 1) \/)]
      (fs/mkdirs (str target-dir (subs route 0 (inc last-index)))))
    (spit path content)))

(defn- build-sites!
  "Builds all sites."
  [{:keys [sites] :as config}]
  (doseq [site-key (keys sites)]
    (build-site! site-key config)))

(defn build-web!
  "Builds a static web for the given config."
  [config]
  (copy-resources! config)
  (build-sites! config)
  (println "All done in build."))