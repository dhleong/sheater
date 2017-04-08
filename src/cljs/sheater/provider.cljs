(ns ^{:author "Daniel Leong"
      :doc "provider"}
  sheater.provider
  (:require [sheater.provider.gapi.view :as gapi-view]
            [sheater.provider.gapi :refer [->GapiProvider]]))

(def providers
  {:gapi
   {:name "Google Drive"
    :id :gapi
    :panel gapi-view/main-panel
    :inst (->GapiProvider)}})

