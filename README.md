# Building static web with hot-code reloading

![Volcano](doc/volcano.png)

[![Clojars Project](https://img.shields.io/clojars/v/orgpad/volcano.svg)](https://clojars.org/orgpad/volcano)

> Remind me that the most fertile lands were built by the fires of volcanoes.”
> ― Andrea Gibson, The Madness Vase

Well, we really love hot-code reloading while developing single page applications (SPA) in ClojureScript.
It was introduced by Figwheel, and well, if you haven't seen these amazing videos you should watch them:
[Figwheel introduction](https://www.youtube.com/watch?v=j-kj2qwJa_E) and
[coding Flappy Bird](https://www.youtube.com/watch?v=KZjFVdU8VLI). We mean it, you should go to watch them right now!
In [OrgPad](https://orgpad.com), we were happily using this in development and didn't even have data persistence for
the first two months.

We wanted to rebuild our [landing page](https//orgpad.com/about). The requirements are that it should consist of a
few static webpages linked together, with the minimum amount of Javascript and CSS, so they load really fast even on
a slow mobile connection. We wanted to use Clojure(Script) to be able to connect them to the rest of our codebase.
So we were originally generating HTML using [Hiccup library](https://github.com/weavejester/hiccup). You do a change,
you reload the file in REPL, then you reload the browser, and you see the result. It's incredibly slow and tedious
if you are used to instant code reloading from your SPA. If you use CSS generators such as
[Less](https://github.com/montoux/lein-less) or [Garden](https://github.com/noprompt/lein-garden), you also have to
reload the browser after every change.

We were looking into existing Clojure solutions: [Stasis](https://github.com/magnars/stasis),
[Oz](https://github.com/metasoarous/oz), and a few others. They were either build for a different purpose:
generating large webs, generating easy blogs, scientific visualizations. They were difficult to set up. They were
either very restrictive or just giving a few functions which you should use to build your own infrastructure. Hot-code
reloading and auto-updating of CSS files which we wanted was not included. Therefore, we build Volcano which is
a microframework for generating static web. If you are familiar with Clojure(Script), you should get it running in
5 minutes.

## How it works

You just write a single config map describing your entire web. Each site is a sequence in data in Hiccup format.
In development, Volcano runs your Web as a SPA using ClojureScript, [Shadow-cljs](http://shadow-cljs.org/) and
[Reagent](https://github.com/reagent-project/reagent). When you do any changes to the config map, you see them
immediately. For production, Volcano builds static HTML files of your Web using Clojure which you can deploy online
or further process. Since your code is runned by both Clojure and ClojureScript, it has to be written in .cljc files.

## Getting started

Include this dependency into your project:

[![Clojars Project](http://clojars.org/orgpad/volcano/latest-version.svg)](http://clojars.org/orgpad/vulcano)

### Defining site config

Somewhere in a .cljc file, define a config map:

```clojure
(ns my-web.config
  (:require [bidi.bidi :as b]))

(def routes
  ["" [["/index.html" :site/index]
       ["/contact.html" :site/contact]]])

(def index
  (list
    [:h1.colored "Index"]
    [:div {:style {:color "green"}} "Some introductory text: "
     [:a {:href (b/path-for routes :site/contact)} "Go to contacts"]]
    [:ul
     (for [index (range 10)]
       [:li "Element " (inc index)])]))

(def contact
  (list
    [:h1 "Contact"]
    [:div "My email address is " [:b "info@orgpad.com"]]
    [:a {:href (b/path-for routes :site/index)} "Back to index"]))

(def config
  {:resource-dir     "resources"
   :target-dir       "build"
   :sites            {:site/index   {:hiccups index}
                      :site/contact {:hiccups contact}}
   :routes           routes
   :default-route    :site/index
   :default-template [:html
                      [:head
                       [:title "Your website title"]
                       [:meta {:charset "utf-8"}]
                       [:link {:href "css/website.css" :rel "stylesheet" :type "text/css"}]]
                      [:body :volcano/hiccups]]
   :exclude-files    #{"index.html"}
   :exclude-dirs     #{"js"}})
```

We are defining routing for our website, giving hiccup for two sites and putting everything together into a single
config map.

### Live code reloading in development

To do live code reloading in development, write the following code in a .cljs file:

```clojure
(ns my-web.dev
  (:require [volcano.dev :as volcano]
            [reagent.dom :as r-dom]
            [my-web.config :as config]))

(defn mount-root
  "Rendering of the current site inside :div#app element."
  []
  (r-dom/render [volcano/render config/config]
                (.getElementById js/document "app")))

(defn init
  "Init function of the dev."
  []
  (volcano/set-routing! config/config)
  (mount-root))
```

Inside resources, add `index.html` having the following:

```html
<!doctype html>
<html lang="en">
<head>
    <title>Klavik.cz - dev</title>
    <meta charset='utf-8'>
    <link href="/css/klavik.css" rel="stylesheet" type="text/css">
</head>
<body>
<div id="app"></div>
<script src="/js/main.js"></script>
</body>
</html>

```

Also write your `shadow-cljs.edn` file looking like this:

```clojure
{:source-paths ["src"]
 :dependencies [[reagent "0.10.0"]
                [bidi "2.1.6"]
                [orgpad/volcano "0.1.0-SNAPSHOT"]
                [venantius/accountant "0.2.5"]
                [binaryage/devtools "0.9.10"]]
 :nrepl        {:port 9500}
 :builds       {:client {:target     :browser
                         :output-dir "resources/js"
                         :asset-path "/js"
                         :modules    {:main {:init-fn my-web.dev/init}}
                         :devtools   {:http-root      "resources"
                                      :http-port      3500
                                      :after-load     my-web.dev/mount-root
                                      :watch-dir      "resources"
                                      :browser-inject :main}}}}
```

You run it in development as:

```bash
shadow-cljs watch client
```

Open [http://localhost:3500](http://localhost:3500) in your browser to see the website immediately. If you click on
a link, it will change the current site shown. Only when you change routing or add a new site, you will need to reload
the page.

Also, if you update any used CSS file inside `resources`, for instance by running
[Lein Less](https://github.com/montoux/lein-less) or [Garden](https://github.com/noprompt/lein-garden),
the changes will immediately show in the browser.

### Building static web for production

You just call this function from Clojure:

```clojure
(build/build-web! config/config)
```

You can call it from REPL or put it inside `-main` and running it via `lein run`. It will copy the non-excluded static
resources to the build directory. Then, it builds a single html file for each defined site.

## Structure of config map

The following keys are currently used:

*  `:resource-dir` - A path to the resource directory from which static files (images, CSS, etc.) are copied.
*  `:target-dir` - A path in which `build/build-web!` function builds the static web. The directory itself is erased
                   on the start.
*  `:routes` - A [Bidi data structure](https://github.com/juxt/bidi) describing the routes on your web. Not all routes
               have to be used by static websites, so you can easily link your static websites to your SPA.
*  `:sites` - A map from site-id to a map describing a single generated site. Each site-id is a keyword. Each such map
              uses these keys:
   * `:hiccups` - A sequence of hiccup, one for each top element in the page. In development, they are inserted inside
                  `<div id="app">` in `index.html`. In production, they replace `:volcano/hiccups` in site template.
   * `:template` - When set, it is used for this site instead of the default template. See below how template works.
*  `:default-template` - An arbitrary hiccup used to generate your site in production. The ocurrence of `:volcano/hiccups`
                         is replaced by the sequence of site's hiccups. Also, resource keys are replaced recursively.
*  `:default-site` - The side-id which is displayed in development when the address does not match any site's route.
*  `:resources` - A map from resource-ids to sequences of hiccups. When a resource-id is used within any hiccup or
                  template, it is replaced by this sequence. The replacement works recursively.
*  `:exclude-dirs` - A set of dirs which are excluded for copying from `:resource-dir` to `:target-dir` in production.
*  `:exclude-files` - A set of files which are excluded, as above.

To inline content of static files into your site, you can load them as resources. Your files have to be placed inside
of your src directory. For instance, suppose that we have `src/my-web/test.txt`. To include its content into the site,
write the following:

```clojure
(ns my-web.config
  (:require [bidi.bidi :as b]
            #?(:cljs [shadow.resource :as resource])
            #?(:clj [clojure.java.io :as io])))

{:resource/test [#?(:clj  (slurp (io/resource "my-web/test.txt"))
                    :cljs (resource/inline "my-web/test.txt"))]}
```

And use `:resource/test` inside your hiccups. When you update `src/my-web/test.txt`, its value is immediately changed
in the browser as well. You can use this to include script, pieces of code, etc. Down the road, we might add markdown
parsing as well.