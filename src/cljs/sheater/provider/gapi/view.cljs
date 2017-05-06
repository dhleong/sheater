(ns ^{:author "Daniel Leong"
      :doc "Google API persistence provider views"}
  sheater.provider.gapi.view
  (:require [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as rc]
            [sheater.provider.gapi :refer [signin! signout!]]))

(defn connect-gapi
  []
  [rc/button
   :class "btn-raised btn-primary"
   :label "Connect Google Account"
   :on-click signin!])

(defn gapi-ready
  []
  [:div "Connected!"
   [:div
    [rc/button
     :class "btn-raised btn-warning"
     :label "Disconnect Google Account"
     :on-click signout!]]])

(defn main-panel
  []
  (let [gapi-info @(subscribe [:provider :gapi])]
    [rc/v-box
     :gap "1em"
     :children
     [[rc/hyperlink-href
       :label "Back to Sheets List"
       :href "#/sheets"]
      (if (:ready? gapi-info)
        [gapi-ready]
        [connect-gapi])]]))
