(ns volcano.dev
  (:require [reagent.core :as r]
            [accountant.core :as accountant]
            [bidi.bidi :as b]
            [volcano.hiccup :as volcano-hiccup]
            [reagent.dom :as r-dom]))

(defonce current-route (r/atom nil))

(defn- update-route
  "Updates the current route according to the path."
  [routes path]
  (when-let [new-route (:handler (b/match-route routes path))]
    (js/console.log "new-route" new-route)
    (reset! current-route new-route)))

(defn- render-nav-bar
  "Renders the navigation bar with all pages."
  [{:keys [pages routes]}]
  [:div {:style {:display          :flex
                 :background-color "black"
                 :justify-content  "center"
                 :width            "100%"
                 :flex-wrap        "wrap"}}
   (doall (for [page-id (sort (keys pages))]
            [:div {:key   page-id
                   :style {:padding     5
                           :font-weight (when (= page-id @current-route) "bold")}}
             [:a {:href  (b/path-for routes page-id)
                  :style {:color "white"}} (str page-id)]]))])

(defn render
  "Renders the page for the current route."
  [{:keys [pages resources nav-bar] :as config}]
  (let [hiccups (:hiccups (get pages @current-route))]
    (into [:<> (when nav-bar
                 (render-nav-bar config))]
          (if hiccups
            (map (partial volcano-hiccup/expand-resources resources nil) hiccups)
            [[:div "Page " @current-route " not found, try to reload the browser!"]]))))

(defn load-scripts!
  "Run this to eval the scripts given by resource-ids in :scripts. These scripts will be available
  during the development."
  [{:keys [resources scripts]}]
  (doseq [script scripts
          :let [code (first (get resources script))]]
    (js/goog.global.eval code)))

(defn set-routing!
  "Sets HTML5 routing and history."
  [{:keys [routes default-route pages]}]
  (let [path (-> js/window .-location .-pathname)]
    (reset! current-route (let [route (:handler (b/match-route routes path))]
                            (if (contains? pages route)
                              route
                              default-route)))
    (accountant/configure-navigation!
      {:nav-handler       (partial update-route routes)
       :path-exists?      #(:handler (b/match-route routes %))
       :reload-same-path? true})))

(defn mount-root!
  "Rendering of the current page inside :div#app element."
  [config]
  (load-scripts! config)
  (r-dom/render [render config] (.getElementById js/document "app")))

(defn init!
  "Init function of the dev."
  [config]
  (set-routing! config)
  (mount-root! config))