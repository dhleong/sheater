(ns ^{:author "Daniel Leong"
      :doc "Sheets list view"}
  sheater.views.sheets
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
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

(defn delete-confirm
  [showing? sheet]
  [rc/modal-panel
   :backdrop-on-click #(reset! showing? nil)
   :wrap-nicely? false
   :child
   [rc/v-box
    :class "modal-content"
    :gap "1em"
    :children
    [[:div.modal-body
      "Are you sure you want to delete the sheet "
      "\"" (:name sheet) "\"?"]
     [:div.modal-body
      [rc/alert-box
       :id "delete-confirm-alert"
       :alert-type :warning
       :body "This CANNOT be undone!"]]
     [:div.modal-footer
      [rc/button
       :label "Cancel"
       :on-click #(reset! showing? nil)]
      [rc/button
       :label "Delete"
       :class "btn-danger"
       :on-click
       (fn []
         (let [provider-id (:provider sheet)]
           (-> providers
               provider-id
               :inst
               (delete-sheet sheet))
           (reset! showing? nil)
           (dispatch [:delete-sheet! (:id sheet)])))]]]]])

(defn panel
  []
  (let [confirming-sheet-delete (reagent/atom nil)]
    (fn []
      (let [sheets @(subscribe [:sheets])]
        [rc/v-box
         :height "100%"
         :children
         [[title]
          (when-let [sheet @confirming-sheet-delete]
            [delete-confirm confirming-sheet-delete sheet])
          [:div.container
           [:div.sheets
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
                  (let [sheet-url (str "#/sheets/" (name (:id sheet)))]
                    ^{:key (:id sheet)}
                    [:tr
                     [:td.clickable
                      {:on-click
                       (fn [e]
                         (.preventDefault e)
                         (dispatch [:navigate! sheet-url]))}
                      [rc/hyperlink-href
                       :label (:name sheet)
                       :href sheet-url]]
                     [:td
                      {:style {:width "5em"}}
                      [rc/md-icon-button
                       :md-icon-name "zmdi-delete"
                       :on-click
                       (fn []
                         (println "prompt delete" (:id sheet))
                         (reset! confirming-sheet-delete sheet))]]]))]])]]]]))))
