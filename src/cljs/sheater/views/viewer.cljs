(ns ^{:author "Daniel Leong"
      :doc "Sheet viewer"}
  sheater.views.viewer
  (:require [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as rc]))

(defn four-oh-four []
  [rc/v-box
   :height "100%"
   :children
   [[rc/title
     :label "Oops!"
     :level :level1]
    [:div "No such sheet"]
    [:div
     [rc/hyperlink-href
      :label "Go back to the list"
      :href "#/sheets"]]]])

(defn render-sheet
  [info]
  (when-not (:data info)
    (dispatch [:refresh! (:id info)]))
  [rc/v-box
   :height "100%"
   :children
   [[:div "TODO:" info]]])

(defn panel
  [id]
  (let [info @(subscribe [:sheet id])]
    (if info
      [render-sheet info]
      [four-oh-four])))
