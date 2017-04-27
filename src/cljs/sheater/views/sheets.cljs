(ns ^{:author "Daniel Leong"
      :doc "Sheets list view"}
  sheater.views.sheets
  (:require [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as rc]
            [sheater.provider :refer [providers]]
            [sheater.provider.proto :refer [delete-sheet]]
            [sheater.views.header :refer [header-bar]]))

(defn title
  []
  [header-bar
   {:header "sheater"
    :buttons
    [[rc/hyperlink-href
      :label "New Sheet"
      :href "#/sheet/create"]]}])

(defn panel
  []
  (let [sheets @(subscribe [:sheets])]
    [rc/v-box
     :height "100%"
     :children
     [[title]
      [:div.container
       [rc/title
        :label "Pick a sheet"
        :level :level4]
       (when-not (seq sheets)
         [:div "No sheets created. "
          [rc/hyperlink-href
           :label "Create one now!"
           :href "#/sheet/create"]])
       (when (seq sheets)
         [:table.table.table-striped.table-hover
          [:tbody
           (for [sheet sheets]
             ^{:key (:id sheet)}
             [:tr
              [:td
               [rc/hyperlink-href
                :label (:name sheet)
                :href (str "#/sheets/" (name (:id sheet)))]]
              [:td
               [rc/md-icon-button
                :md-icon-name "zmdi-delete"
                :on-click
                (fn []
                  (let [provider-id (:provider sheet)]
                    (-> providers
                        provider-id
                        :inst
                        (delete-sheet sheet))
                    (dispatch [:delete-sheet! (:id sheet)])))]]])]])]]]))
