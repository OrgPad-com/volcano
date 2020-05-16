(defproject orgpad/volcano "0.1.2"
  :description "Static web generator with hot-code reloading in development"
  :url "https://github.com/OrgPad-com/volcano"
  :license {:name "The MIT License"
            :url  "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [org.clojure/clojurescript "1.10.742" :scope "provided"]
                 [bidi "2.1.6"]
                 [hiccup "1.0.5"]
                 [me.raynes/fs "1.4.6"]
                 [reagent "0.10.0"]
                 [venantius/accountant "0.2.5"]]
  :min-lein-version "2.5.3"
  :source-paths ["src"]
  :deploy-repositories [["clojars" {:sign-releases false
                                    :url           "https://clojars.org/repo"}]])