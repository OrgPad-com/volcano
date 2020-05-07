(ns volcano.build
  (:require [me.raynes.fs :as fs]
            [hiccup.core :as hiccup]
            [hiccup.page :as page]
            [volcano.hiccup :as volcano-hiccup]
            [bidi.bidi :as b]
            [clojure.string :as str]))

(defn- copy-resources!
  "Copies all non-excluded files from resource-dir to target-dir."
  [{:keys [resource-dir target-dir exclude-files exclude-dirs]}]
  (println "Copying static resources to" target-dir "...")
  (fs/delete-dir target-dir)
  (fs/copy-dir resource-dir target-dir)
  (doseq [file exclude-files]
    (fs/delete (str target-dir "/" file)))
  (doseq [dir exclude-dirs]
    (fs/delete-dir (str target-dir "/" dir))))

(defn- generate-hiccup
  "Generates a hiccup from the given template with inserted hiccups, expanded resources and styles."
  [page-id {:keys [default-template resources routes pages] :as config}]
  (let [{:keys [hiccups template] :or {template default-template}} (get pages page-id)]
    (->> template (volcano-hiccup/expand-resources (assoc resources :volcano/hiccups hiccups))
         (volcano-hiccup/replace-paths (b/path-for routes page-id) config)
         volcano-hiccup/expand-styles)))

(defn- build-page!
  "Build a single page-id of the given keyword and hiccup."
  [page-id {:keys [pages routes target-dir] :as config}]
  (println "Building site" page-id "...")
  (let [{:keys [output-path]} (get pages page-id)
        route (or output-path (b/path-for routes page-id))
        path (str target-dir route)
        content (hiccup/html {:mode :html}
                             (page/doctype :html5)
                             (generate-hiccup page-id config))]
    (when-let [last-index (str/last-index-of (subs route 1) \/)]
      (fs/mkdirs (str target-dir (subs route 0 (inc last-index)))))
    (spit path content)))

(defn- build-pages!
  "Builds all sites."
  [{:keys [pages] :as config}]
  (doseq [page-id (keys pages)]
    (build-page! page-id config)))

(defn build-web!
  "Builds a static web for the given config."
  [config]
  (copy-resources! config)
  (build-pages! config)
  (println "All done in build."))