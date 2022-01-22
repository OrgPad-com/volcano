(defproject orgpad/volcano "0.2.1"
  :description "Static web generator with hot-code reloading in development"
  :url "https://github.com/OrgPad-com/volcano"
  :license {:name "The MIT License"
            :url  "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojurescript "1.10.914" :scope "provided"]
                 [bidi "2.1.6"]
                 [reagent "0.10.0"]
                 [venantius/accountant "0.2.5"]
                 [binaryage/devtools "0.9.10"]]
  :min-lein-version "2.5.3"
  :source-paths ["src"]
  :deploy-repositories [["clojars" {:sign-releases false
                                    :url           "https://clojars.org/repo"}]])