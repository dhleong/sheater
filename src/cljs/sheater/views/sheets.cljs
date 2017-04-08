(ns ^{:author "Daniel Leong"
      :doc "Sheets list view"}
  sheater.views.sheets
  (:require [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as rc]
            [sheater.provider :refer [providers]]
            [sheater.provider.proto :refer [delete-sheet]]))

(defn title []
  [rc/title
   :label "Sheater"
   :level :level1])

(defn panel
  []
  (let [sheets @(subscribe [:sheets])]
    [rc/v-box
     :height "100%"
     :children
     [[title]
      (if-not (seq sheets)
        [:div "No sheets created. "
         [rc/hyperlink-href
          :label "Create one now!"
          :href "#/sheet/create"]]
        [rc/hyperlink-href
          :label "New sheet"
          :href "#/sheet/create"])
      (when (seq sheets)
        [:ul
         (for [sheet sheets]
           ^{:key (:id sheet)}
           [:li
            [rc/h-box
             :children
             [[rc/hyperlink-href
               :label (:name sheet)
               :href (str "#/sheets/" (name (:id sheet)))]
              [rc/md-icon-button
               :md-icon-name "zmdi-delete"
               :on-click
               (fn []
                 (let [provider-id (:provider sheet)]
                   (-> providers
                       provider-id
                       :inst
                       (delete-sheet sheet))
                   (dispatch [:delete-sheet! (:id sheet)])))]]]])])]]))
