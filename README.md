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

We wanted to rebuild our [landing page](https://orgpad.com/about). The requirements are that it should consist of a
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

You just write a single config map describing your entire website. Each page is a sequence of data in Hiccup format.
In development, Volcano runs your Web as a SPA using ClojureScript, [Shadow-cljs](http://shadow-cljs.org/) and
[Reagent](https://github.com/reagent-project/reagent). When you do any changes to the config map, you see them
immediately. For production, Volcano builds static HTML files of your Web using Clojure which you can deploy online
or further process. Since your code is runned by both Clojure and ClojureScript, it has to be written in .cljc files.

## Quick start

You can use [Volcano Leiningen template](https://github.com/OrgPad-com/volcano-template) to quickly start a static
website project:

```shell script
lein new volcano <project-name>
```

Use optional `+less` or `+garden` for CSS generators. You will get a project with two example pages and all
configuration needed to run Volcano. Consult the generated README.md.

## Setting up on your own

Next, we explain how to setup Volcano as part of your existing project. First, include this dependency into your
project:

[![Clojars Project](http://clojars.org/orgpad/volcano/latest-version.svg)](http://clojars.org/orgpad/vulcano)

### Defining website config

Somewhere in a .cljc file, define a config map:

```clojure
(ns my-web.config
  (:require [bidi.bidi :as b]))

(def routes
  ["" [["/index.html" :page/index]
       ["/contact.html" :page/contact]]])

(defn index []
  (list
    [:h1.colored "Index"]
    [:div {:style {:color "green"}} "Some introductory text: "
     [:a {:href (b/path-for routes :page/contact)} "Go to contacts"]]
    [:ul
     (for [index (range 10)]
       [:li "Element " (inc index)])]))

(defn contact []
  (list
    [:h1 "Contact"]
    [:div "My email address is " [:b "info@orgpad.com"]]
    [:a {:href (b/path-for routes :page/index)} "Back to index"]))

(defn config []
  {:resource-dir     "resources"
   :target-dir       "build"
   :pages            {:page/index   {:hiccups (index)}
                      :page/contact {:hiccups (contact)}}
   :routes           routes
   :default-route    :page/index
   :default-template [:html
                      [:head
                       [:title "Your website title"]
                       [:meta {:charset "utf-8"}]
                       [:link {:href "/css/my-web.css" :rel "stylesheet" :type "text/css"}]]
                      [:body :volcano/hiccups]]
   :exclude-files    #{"index.html"}
   :exclude-dirs     #{"js"}})
```

We are defining routing for our website using [Bidi](https://github.com/juxt/bidi), giving hiccup for two pages
and putting everything together into a single config map.

### Live code reloading in development

To do live code reloading in development, write the following code in a .cljs file:

```clojure
(ns my-web.dev
  (:require [volcano.dev :as volcano]
            [reagent.dom :as r-dom]
            [my-web.config :as config]))

(defn mount-root
  "Rendering of the current page inside :div#app element."
  []
  (volcano/load-scripts! (config/config))
  (r-dom/render [volcano/render (config/config)]
                (.getElementById js/document "app")))

(defn init
  "Init function of the dev."
  []
  (volcano/set-routing! (config/config))
  (mount-root))
```

The function `init` is called when dev is loaded in the browser, setting the HTML5 routing and history. Everytime a code
is changed, `mount-root` function is called, rerendering the page content.

Inside `resources` subdirectory, add `index.html` template having the following:

```html
<!doctype html>
<html lang="en">
<head>
    <title>My-web - dev</title>
    <meta charset='utf-8'>
    <link href="/css/my-web.css" rel="stylesheet" type="text/css">
</head>
<body>
<div id="app"></div>
<script src="/js/main.js"></script>
</body>
</html>
```

If you are unfamiliar with [Shadow-cljs](http://shadow-cljs.org/), you need to install [NodeJS](https://nodejs.org/en/).
After that, run the following in the project directory:

```shell script
npm install -g shadow-cljs
npm install react
npm install react-dom
npm install create-react-class
```

Write your `shadow-cljs.edn` file looking like this:

```clojure
{:source-paths ["src"]
 :dependencies [[reagent "0.10.0"]
                [bidi "2.1.6"]
                [orgpad/volcano "0.1.0-SNAPSHOT"]
                [venantius/accountant "0.2.5"]
                [binaryage/devtools "0.9.10"]]
 :nrepl        {:port 9500}
 :builds       {:web {:target     :browser
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

```shell script
shadow-cljs watch web
```

Open [http://localhost:3500](http://localhost:3500) in your browser to see the website immediately. If you click on
a link, it will change the current page shown. Only when you change routing or add a new page, you will need to reload
the browser. It also runs you nREPL server on port 9500 which is great for interactive development. 

Also, if you update any used CSS file inside `resources`, for instance by running
[Lein Less](https://github.com/montoux/lein-less) or [Garden](https://github.com/noprompt/lein-garden),
the changes will immediately show in the browser.

If your code is spread through multiple namespaces, we recommend either using functions with zero arguments instead of
defining symbols, or using Vars: `#'config` instead of `config`. Otherwise hot-code reloading might not propagate
changes correctly. Alternatively, you can add `:reload-strategy :full` into `:devtools` in `shadow-cljs.edn`, but for
a larger web it might slow down everything.

### Building static web for production

You just call this function from Clojure:

```clojure
(build/build-web! (config/config))
```

You can call it from REPL or put it inside `-main` and running it via `lein run` (as done in the template). It will copy
the non-excluded static resources to the build directory. Then, it builds a single html file for each defined page.
For the config above, we get the following html files (pretty printed):

#### index.html

```html
<html>
  <head>
    <title>Your website title</title>
    <meta charset="utf-8" />
    <link href="/css/my-web.css" rel="stylesheet" type="text/css" />
  </head>
  <body>
    <h1 class="colored">Index</h1>
    <div style="color:green">Some introductory text: <a href="/contact.html">Go to contacts</a></div>
    <ul>
      <li>Element 1</li>
      <li>Element 2</li>
      <li>Element 3</li>
      <li>Element 4</li>
      <li>Element 5</li>
      <li>Element 6</li>
      <li>Element 7</li>
      <li>Element 8</li>
      <li>Element 9</li>
      <li>Element 10</li>
    </ul>
  </body>
</html>
```

#### contact.html

```html
<html>
  <head>
    <title>Your website title</title>
    <meta charset="utf-8" />
    <link href="/css/my-web.css" rel="stylesheet" type="text/css" />
  </head>
  <body>
    <h1>Contact</h1>
    <div>My email address is <b>info@orgpad.com</b></div>
    <a href="/index.html">Back to index</a>
  </body>
</html>
```
You can call it from REPL or put it inside `-main` and running it via `lein run` (as done in the template). It will copy
the non-excluded static resources to the build directory. Then, it builds a single html file for each defined page.

## Structure of config map

The following keys are currently used:

*  `:resource-dir` - A path to the resource directory from which static files (images, CSS, etc.) are copied.
*  `:target-dir` - A path in which `build/build-web!` function builds the static web. The directory itself is erased
                   on the start.
*  `:routes` - A [Bidi data structure](https://github.com/juxt/bidi) describing the routes on your web. Not all routes
               have to be used by static pages, so you can easily link your static websites to your SPA.
*  `:pages` - A map from pages-id to a map describing a single generated page. Each page-id is a keyword. Each such map
              uses these keys:
   * `:hiccups` - A sequence of hiccup, one for each top element in the page. In development, they are inserted inside
                  `<div id="app">` in `index.html`. In production, they replace `:volcano/hiccups` in page template.
                  For styles, you can use Reagent maps, they are automatically expanded into strings, i.e.,
                  `:style {:color "red" :padding 2}` becomes `:style "color:red;paddding:2px"`.
   * `:template` - When set, it is used for this page instead of the default template. See below how template works.
   * `:output-path` - When set, this output path is used instead of the route path. For instance, we might have the
                      route `/intro` but set the output path to `/intro.html`.
*  `:default-template` - An arbitrary hiccup used to generate your page in production. The ocurrence of `:volcano/hiccups`
                         is replaced by the sequence of page's hiccups. Also, resource keys are replaced recursively.
*  `:default-route` - The page-id which is displayed in development when the address does not match any page's route.
*  `:resources` - A map from resource-ids to sequences of hiccups. When a resource-id is used within any hiccup or
                  template, it is replaced by this sequence. The replacement works recursively.
*  `:scripts` - A sequences of resource-ids whose code is evaluated in development, so it can be tested in development.
*  `:exclude-dirs` - A set of dirs which are excluded for copying from `:resource-dir` to `:target-dir` in production.
*  `:exclude-files` - A set of files which are excluded, as above.
*  `:relative-paths` - When set, the absolute paths are replaced by relative paths.
*  `:path-prefix` - It replaces the absolute path prefix `/`.
*  `:nav-bar` - When set true, a navigation bar for all pages is placed at the top of the pages in development. This is
                useful when the pages are not linked together (for instance, when generating email templates).

### Loading static files

Since the development runs as a SPA in browser, we cannot easily load files from disk. But we can use
`shadow.resource/inline` macro which loads the file in compilation time and inlines is content as a string.
Your file has to be placed inside of your src directory. For instance, suppose that we have `src/my-web/test.txt`.
To get its content, call the following:

```clojure
(resource/inline "my-web/test.txt")
```

We can use `:resources` map to easily include the loaded content into your page. We can store it using the resource-id
`:resource/test` in `:resources` map and place this id anywhere inside your hiccups or templates. When you update
`src/my-web/test.txt`, its value is immediately changed in the browser as well. You can use this to include script,
pieces of code, etc. Down the road, we might add markdown parsing as well.

### Including scripts

To include external scripts, we can just add `<script>` tag with `src` inside your HTML. For development, you can add
it to `index.html`, even if it is not needed for all generated pages. For static website, you can add it inside your
page templates.

To include inline scripts, you need to eval them in development to be able to access them from ClojureScript. Load your
inline script from a file as a resource and add its resource-id into `:scripts`. In development, the script will get
evaluated during hot code reloading whenever any code is updated. For building static websites, include this resource-id
into your template.

To attach a JS function to an event, you have to write different code for development and for building the static
website, using the reader literals. In development, attach a ClojureScript function calling the JS function using
JS interop. For building static website, just call the JS function from string.

```clojure
[:div.button #?(:clj  {:onclick "send();"}
                :cljs {:on-click #(js/send)})
  "Send"]
```

## Example websites

Have you built something with Volcano? Let us know, so we can add it here:

*   [OrgPad landing page](https://orgpad.com/l/intro)
*   [Law and accountant office Klavík](https://www.klavik.cz)
*   [Jiří Šmíd's personal web](https://www.smid-interim.cz/)
