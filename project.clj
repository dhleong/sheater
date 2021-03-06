(defproject sheater "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.521"]
                 [reagent "0.6.1" :exclusions [cljsjs/react-dom cljsjs/react-dom-server]]
                 [cljsjs/react-dom "15.4.2-2"] ; manually declare for now to fix issue with number inputs
                 [cljsjs/react-dom-server "15.4.2-2"]
                 [re-frame "0.9.2"]
                 [org.clojure/core.async "0.2.391"]
                 [re-com "2.0.0"]
                 [secretary "1.2.3"]
                 [cljs-ajax "0.5.9"]]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-less "1.7.5"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljc" "src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"]

  :figwheel {:css-dirs ["resources/public/css"]
             :server-port 8080}

  :doo {:build "test"}

  :test-paths ["test/cljs"]

  :less {:source-paths ["resources/public/css"]
         :target-path  "resources/public/css"}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.8.2"]
                   [figwheel-sidecar "0.5.9"]
                   [com.cemerick/piggieback "0.2.1"]

                   [doo "0.1.7"]]

    :plugins      [[lein-figwheel "0.5.9"]
                   [lein-doo "0.1.7"]]
    }}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/clj" "src/cljc" "src/cljs"]
     :figwheel     {:on-jsload "sheater.core/mount-root"}
     :compiler     {:main                 sheater.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}
                    }}

    {:id           "min"
     :source-paths ["src/clj" "src/cljc" "src/cljs"]
     :compiler     {:main            sheater.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false
                    ; these flags are useful when debugging :advanced issues:
                    ;; :pretty-print    true
                    ;; :pseudo-names    true

                    :externs ["externs/gapi.js"
                              "externs/sheater.js"]}}

    {:id           "test"
     :source-paths ["src/clj" "src/cljc" "src/cljs" "test/cljs"]
     :compiler     {:main          sheater.runner
                    :output-to     "resources/public/js/compiled/test.js"
                    :output-dir    "resources/public/js/compiled/test/out"
                    :optimizations :none}}
    ]}

  )
