(ns ^{:author "Daniel Leong"
      :doc "Google API persistence provider"}
  sheater.provider.gapi
  (:require [re-frame.core :refer [dispatch]]))


;; Client ID and API key from the Developer Console
(def client-id "772789905450-1m5cl9pf81bknttli2lma04qjalqcv4m.apps.googleusercontent.com")

;; Array of API discovery doc URLs for APIs used by the quickstart
(def discovery-docs ["https://www.googleapis.com/discovery/v1/apis/drive/v3/rest"])

;; Authorization scopes required by the API; multiple scopes can be
;; included, separated by spaces.
(def scopes "https://www.googleapis.com/auth/drive.metadata.readonly")

(defn- update-signin-status!
  [signed-in?]
  (println "signed-in? <-" signed-in?)
  (dispatch [:assoc-provider! :gapi :ready? signed-in?]))

(defn- on-client-init
  []
  (js/console.log "gapi client init!")
  (let [auth-instance
        (.getAuthInstance js/gapi.auth2)]
    ; listen for updates
    (-> auth-instance
        (.-isSignedIn)
        (.listen update-signin-status!))
    ; set current status immediately
    (update-signin-status!
      (-> auth-instance
          (.-isSignedIn)
          (.get)))))

(defn init-client!
  []
  (js/console.log "init-client!")
  (-> (js/gapi.client.init
        #js {:discoveryDocs discovery-docs
             :clientId client-id
             :scope scopes})
      (.then on-client-init)))

(defn handle-client-load
  []
  (js/console.log "handle-client-load")
  (js/gapi.load "client:auth2", init-client!))
