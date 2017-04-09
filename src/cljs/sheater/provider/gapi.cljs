(ns ^{:author "Daniel Leong"
      :doc "Google API persistence provider"}
  sheater.provider.gapi
  (:require [clojure.string :as str]
            [cljs.reader :as edn]
            [re-frame.core :refer [dispatch]]
            [sheater.provider.proto :refer [IProvider]]))

;;
;; Constants
;;

;; Client ID and API key from the Developer Console
(def client-id "772789905450-1m5cl9pf81bknttli2lma04qjalqcv4m.apps.googleusercontent.com")

;; Array of API discovery doc URLs for APIs used by the quickstart
(def discovery-docs #js ["https://www.googleapis.com/discovery/v1/apis/drive/v3/rest"])

;; Authorization scopes required by the API; multiple scopes can be
;; included, separated by spaces.
(def scopes (str/join
              " "
              ["https://www.googleapis.com/auth/drive.appfolder"
               "https://www.googleapis.com/auth/drive.appdata"]))

;;
;; Internal util
;;

(defn ->id
  [gapi-id]
  (keyword
    (str "gapi-" gapi-id)))

(defn ->sheet
  [gapi-id sheet-name]
  {:provider :gapi
   :id (->id gapi-id)
   :name sheet-name
   :gapi-id gapi-id})

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
                       (->sheet (:id raw-file)
                                (:name raw-file)))))]
    (println "Found: " files)
    (dispatch [:add-sheets files])))

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

(defn upload-data
  "The GAPI client doesn't provide proper support for
  file uploads out-of-the-box, so let's roll our own"
  [upload-type metadata content on-complete]
  {:pre [(contains? #{:create :update} upload-type)
         (string? (:mimeType metadata))
         (not (nil? content))]}
  (let [base (case upload-type
               :create {:path "/upload/drive/v3/files"
                        :method "POST"}
               :update {:path (str "/upload/drive/v3/files/"
                                   (:fileId metadata))
                        :method "PATCH"})
        boundary "-------314159265358979323846"
        delimiter (str "\r\n--" boundary "\r\n")
        close-delim (str "\r\n--" boundary "--")
        body (str delimiter
                  "Content-Type: application/json\r\n\r\n"
                  (js/JSON.stringify (clj->js metadata))
                  delimiter
                  "Content-Type: " (:mimeType metadata) "\r\n\r\n"
                  content
                  close-delim)
        request (assoc base
                       :params {:uploadType "multipart"}
                       :headers
                       {:Content-Type
                        (str "multipart/related; boundary=\"" boundary "\"")}
                       :body body)]
    (.execute
      (js/gapi.client.request
        (clj->js request))
      on-complete)))

(defn upload-data-with-retry
  [upload-type metadata content on-complete]
  (upload-data
    upload-type metadata content
    (fn [resp]
      (let [error (.-error resp)]
        (println "upload-data ERROR:" error)
        (if (and error
                 (= 401 (.-code error)))
          ; refresh creds and retry
          (js/gapi.auth.authorize
            #js {:client_id client-id
                 :scope scopes
                 :immediate true}
            (fn [refresh-resp]
              (if (.-error refresh-resp)
                (do
                  (println "Auth refresh failed:" refresh-resp)
                  (on-complete resp))
                (do
                  (println "Auth refreshed:" refresh-resp)
                  (upload-data
                    upload-type metadata content
                    on-complete)))))
          ; no problem; pass it along
          (on-complete resp))))))

(deftype GapiProvider []
  IProvider
  (create-sheet [this info on-complete]
    (upload-data-with-retry
      :create
      {:name (:name info)
       :mimeType "application/json"
       :parents ["appDataFolder"]}
      (str
        {:name (:name info)
         ; TODO:
         :pages
         [{:name "Main"
           :spec []}
          {:name "Notes"
           :type :notes}]})
      (fn [response]
        (println "CREATED:" response)
        (let [id (-> response
                     (js->clj :keywordize-keys true)
                     :id)]
          (on-complete
            (->sheet id
                     (:name info)))))))
  ;
  (delete-sheet [this info]
    (println "Delete " (:gapi-id info))
    (-> js/gapi.client.drive.files
        (.delete #js {:fileId (:gapi-id info)})
        (.then (fn [resp]
                 (println "Deleted!" (:gapi-id info)))
               (fn [e]
                 (js/console.warn "Failed to delete " (:gapi-id info))))))
  ;
  (refresh-sheet [this info on-complete]
    (println "Refresh " (:gapi-id info))
    (-> (js/gapi.client.drive.files.get
          #js {:fileId (:gapi-id info)
               :alt "media"})
        (.then (fn [resp]
                 (js/console.log resp)
                 (when-let [body (.-body resp)]
                   (when-let [data (edn/read-string body)]
                     (on-complete data))))
               (fn [e]
                 (println "ERROR listing files" e)))))
  ;
  (save-sheet [this info]
    (println "Save " (:gapi-id info))
    (println (str (:data info)))
    (upload-data-with-retry
      :update
      {:fileId (:gapi-id info)
       :mimeType "application/json"}
      (str (:data info))
      (fn [response]
        (println "SAVED!" response)))))
