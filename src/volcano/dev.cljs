(ns volcano.dev
  (:require [reagent.core :as r]
            [accountant.core :as accountant]
            [bidi.bidi :as b]
            [volcano.hiccup :as volcano-hiccup]))

(defonce current-route (r/atom nil))

(defn- update-route
  "Updates the current route according to the path."
  [routes path]
  (when-let [new-route (:handler (b/match-route routes path))]
    (js/console.log "new-route" new-route)
    (reset! current-route new-route)))

(defn render
  "Renders the site for the current route."
  [{:keys [sites resources]}]
  (let [hiccups (:hiccups (get sites @current-route))]
    (into [:<>] (if hiccups
                  (map (partial volcano-hiccup/expand-resources resources) hiccups)
                  [[:div "Site " @current-route " not found, try to reload the page!"]]))))

(defn set-routing!
  "Sets HTML5 routing and history."
  [{:keys [routes default-route]}]
  (let [path (-> js/window .-location .-pathname)]
    (reset! current-route (or (:handler (b/match-route routes path)) default-route))
    (accountant/configure-navigation!
      {:nav-handler       (partial update-route routes)
       :path-exists?      #(:handler (b/match-route routes %))
       :reload-same-path? true})))