(ns ^{:author "Daniel Leong"
      :doc "Sheet viewer"}
  sheater.views.viewer
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as rc]
            [sheater.views.header :refer [header-bar]]
            [sheater.views.pages.custom-page :as custom-page]
            [sheater.views.pages.notes-page :as notes-page]))

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

(defn render-page
  [page-info state]
  (if-let [page-type (:type page-info)]
    (case page-type
      :notes [notes-page/render (:name page-info) state]
      [rc/alert-box
       :alert-type :danger
       :body (str "Unknown page type: " page-type)])
    [custom-page/render page-info state]))

(defn render-sheet
  [page info]
  (let [data (:data info)
        page (or page
                 (when data
                   (-> data :pages first :name)))]
    (when-not data
      ; don't got it? get it!
      (dispatch [:refresh! (:id info)]))
    [rc/v-box
     :height "100%"
     :children
     (if-not data
       [[rc/throbber
         :size :large]]
       ;
       [[header-bar
         {:header [:span.navbar-collapse.collapse
                   (:name info)]
          :<-up-to "#/sheets"
          :tabs
          (map
            (fn [p]
              {:label (:name p)
               :active? (= (:name p) page)
               :url (str "#/sheets/" (name (:id info)) "/" (:name p))})
            (:pages data))
          :buttons
          [[rc/hyperlink-href
            :label "Edit"
            :href (str "#/edit/"
                       (name (:id info))
                       "/"
                       page)]]}]
        [:div.container
         [render-page
         (->> data
              :pages
              (filter (comp (partial = page) :name))
              first)
         @(subscribe [:active-state])]]])]))

(defn panel
  [[id page]]
  (let [info @(subscribe [:sheet id])
        any-loading? @(subscribe [:any-loading?])]
    (cond
      info [render-sheet page info]
      any-loading? [rc/throbber
                    :size :large]
      :else [four-oh-four])))
