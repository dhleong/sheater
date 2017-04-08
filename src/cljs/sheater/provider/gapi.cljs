(ns ^{:author "Daniel Leong"
      :doc "Google API persistence provider"}
  sheater.provider.gapi
  (:require [re-frame.core :refer [dispatch]]))

;;
;; Constants
;;

;; Client ID and API key from the Developer Console
(def client-id "772789905450-1m5cl9pf81bknttli2lma04qjalqcv4m.apps.googleusercontent.com")

;; Array of API discovery doc URLs for APIs used by the quickstart
(def discovery-docs #js ["https://www.googleapis.com/discovery/v1/apis/drive/v3/rest"])

;; Authorization scopes required by the API; multiple scopes can be
;; included, separated by spaces.
(def scopes "https://www.googleapis.com/auth/drive.appfolder")

;;
;; State management and API interactions
;;

(defn- auth-instance
  "Convenience to get the gapi auth instance:
  gapi.auth2.getAuthInstance()"
  []
  (js/gapi.auth2.getAuthInstance))

(declare on-files-list)
(defn- update-signin-status!
  [signed-in?]
  (println "signed-in? <-" signed-in?)
  (dispatch [:assoc-provider! :gapi :ready? signed-in?])
  (when signed-in?
    (-> (js/gapi.client.drive.files.list
          #js {:pageSize 50
               :spaces "appDataFolder"
               :fields "nextPageToken, files(id, name)"})
        (.then on-files-list
               (fn [e]
                 (println "ERROR listing files" e))))))

(defn on-files-list
  [response]
  (js/console.log "FILES LIST:" response)
  (let [response (js->clj response :keywordize-keys true)
        files (->> response
                   :result
                   :files
                   (map
                     (fn [raw-file]
                       {:provider :gapi
                        :id (keyword
                              (str "gapi/" (:id raw-file)))
                        :name (:name raw-file)
                        :gapi-id (:id raw-file)})))]
    (println "Found: " files)
    (dispatch [:add-files files])))

(defn- on-client-init
  []
  (js/console.log "gapi client init!")
  ; listen for updates
  (-> (auth-instance)
      (.-isSignedIn)
      (.listen update-signin-status!))
  ; set current status immediately
  (update-signin-status!
    (-> (auth-instance)
        (.-isSignedIn)
        (.get))))

(defn init-client!
  []
  (js/console.log "init-client!")
  (-> (js/gapi.client.init
        #js {:discoveryDocs discovery-docs
             :clientId client-id
             :scope scopes})
      (.then on-client-init)))

;;
;; NOTE: Exposed to index.html
(defn handle-client-load
  []
  (js/console.log "handle-client-load")
  (js/gapi.load "client:auth2", init-client!))

;;
;; Public API
;;

(defn signin!
  []
  (-> (auth-instance)
      (.signIn)))

(defn signout!
  []
  (-> (auth-instance)
      (.signOut)))
