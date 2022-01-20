(ns volcano.build
  (:require ["fs" :as fs]
            [bidi.bidi :as b]
            [reagent.dom.server :as rdom-server]
            [clojure.string :as str]
            [volcano.hiccup :as volcano-hiccup]))

(defn copy-resources!
  "Copies all non-excluded files from resource-dir to target-dir."
  [{:keys [resource-dir target-dir exclude-files exclude-dirs]}]
  (println "Copying static resources to" target-dir "...")
  (when (fs/existsSync target-dir)
    (fs/rmSync target-dir #js{:recursive true}))
  (when (fs/existsSync resource-dir)
    (fs/cpSync resource-dir target-dir #js{:recursive true}))
  (doseq [file exclude-files
          :let [path (str target-dir "/" file)]]
    (when (fs/existsSync path)
      (fs/rmSync path)))
  (doseq [dir exclude-dirs
          :let [path (str target-dir "/" dir)]]
    (when (fs/existsSync path)
      (fs/rmSync path #js{:recursive true}))))

(defn- generate-hiccup
  "Generates a hiccup from the given template with inserted hiccups, expanded resources and styles."
  [page-id {:keys [default-template resources routes pages] :as config}]
  (let [{:keys [hiccups template] :or {template default-template}} (get pages page-id)]
    (->> template (volcano-hiccup/expand-resources (assoc resources :volcano/hiccups hiccups))
         (volcano-hiccup/replace-paths (b/path-for routes page-id) config))))

(defn- build-page!
  "Build a single page-id of the given keyword and hiccup."
  [page-id {:keys [pages routes target-dir] :as config}]
  (println "Building page" page-id "...")
  (let [{:keys [output-path]} (get pages page-id)
        route (or output-path (b/path-for routes page-id))
        path (str target-dir route)
        content (rdom-server/render-to-static-markup (generate-hiccup page-id config))]
    (fs/writeFileSync path content)))

(defn- build-pages!
  "Builds all sites."
  [{:keys [pages] :as config}]
  (doseq [page-id (keys pages)]
    (build-page! page-id config)))

(defn build-web!
  [{:keys [target-dir] :as config}]
  (let [start-time (system-time)]
    (println "Building website into" target-dir "...")
    (copy-resources! config)
    (build-pages! config)
    (println "Build done in" (js/Math.round (- (system-time) start-time)) "ms.")))