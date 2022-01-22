(ns volcano.example.config
  (:require [bidi.bidi :as b]
            [shadow.resource :as resource]))

(def routes
  ["" [["/index.html" :page/index]
       ["/contact.html" :page/contact]]])

(defn index []
  (list
    [:h1.colored "Index"]
    [:img {:width  747
           :height 454
           :src    "/img/volcano.png"}]
    [:div {:style {:color "green"}} "Some introductory text: "
     [:a {:href (b/path-for routes :page/contact)} "Go to contacts"]]
    [:div
     [:button {:on-click       #(js/incrementCounter)
               :volcanoonclick "incrementCounter()"}
      "Click me!"]
     [:span#counter "Button not yet clicked!"]]
    [:ul
     (for [index (range 10)]
       [:li "Element " (inc index)])]))

(defn contact []
  (list
    [:h1.colored "Contact"]
    [:div "My email address is " [:b "info@orgpad.com"]]
    [:a {:href (b/path-for routes :page/index)} "Back to index"]))

(defn config []
  {:resource-dir     "example/resources"
   :target-dir       "example/build"
   :pages            {:page/index   {:hiccups (index)}
                      :page/contact {:hiccups (contact)}}
   :routes           routes
   :default-route    :page/index
   :default-template [:html
                      [:head
                       [:title "Your website title"]
                       [:meta {:charset "utf-8"}]
                       [:link {:href "/css/example.css" :rel "stylesheet" :type "text/css"}]
                       [:script :script/counter]]
                      [:body :volcano/hiccups]]
   :resources        {:script/counter [(resource/inline "volcano/example/counter.js")]}
   :scripts          [:script/counter]
   :exclude-files    #{"index.html"}
   :exclude-dirs     #{"js"}})

