(ns sheater.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [sheater.events]
              [sheater.fx]
              [sheater.subs]
              [sheater.routes :as routes]
              [sheater.views :as views]
              [sheater.config :as config]
              ; ensure providers are loaded
              [sheater.provider.gapi]))


(defn dev-setup []
  (enable-console-print!)
  (when config/debug?
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (js/console.log "core.init")
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))
