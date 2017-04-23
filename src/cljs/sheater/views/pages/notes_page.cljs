(ns ^{:author "Daniel Leong"
      :doc "Notes page"}
  sheater.views.pages.notes-page
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as rc]))

(def tag-regex #"(#([a-zA-Z0-9_-]+))")

(defn extract-tags
  [text]
  (map
    last
    (re-seq tag-regex text)))

(defn tag-cloud
  [tags]
  [:div.tag-cloud
   (for [t tags]
     ^{:key t} [:div.tag t])])

(defn edit-note-dialog
  [showing? note-created]
  (let [note (reagent/atom {:body ""
                            :created (js/Date.now)
                            :tags []})]
    (when-not (nil? note-created)
      (let [notes @(subscribe [:active-notes])
            existing (->> notes
                          (filter
                            (comp (partial = note-created)
                                  :created))
                          first)]
        (when existing
          (println "Found existing: " existing)
          (reset! note existing))))
    (fn []
      [rc/modal-panel
       :backdrop-on-click #(reset! showing? nil)
       :child
       [rc/v-box
        :gap "1em"
        :children
        [[rc/input-textarea
          :model (:body @note "")
          :placeholder "Note body"
          :change-on-blur? false
          :on-change
          (fn [body]
            (swap! note assoc
                   :body body
                   :tags (into #{} (extract-tags body))))]
         [tag-cloud (:tags @note)]
         [rc/button
          :label "Add"
          :on-click
          (fn [e]
            (.preventDefault e)
            (reset! showing? false)
            (println "update-active-note!" @note)
            (dispatch [:update-active-note! @note]))]]]])))

(defn active-tag-cloud
  []
  (let [tags @(subscribe [:active-note-tags])]
    ; TODO
    [tag-cloud tags]))

(defn note-card
  [note]
  [:div.card
   [rc/h-box
    :children
    [[:div (str note)]
     [rc/md-icon-button
      :md-icon-name "zmdi-delete"
      :on-click #(dispatch [:delete-active-note! note])]]]])

(defn search-results
  [filter-atom]
  (let [notes @(subscribe [:search-notes @filter-atom])]
    [rc/v-box
     :children
     (for [n notes]
       ^{:key (:created n)}
       [note-card n])]))

(defn render
  [page state]
  (let [current-filter (reagent/atom "")
        editing-note (reagent/atom nil)]
    (fn []
      [rc/v-box
       :children
       [(when-let [editing @editing-note]
          [edit-note-dialog editing-note editing])
        [rc/input-text
         :model current-filter
         :placeholder "Search"
         :change-on-blur? false
         :on-change
         (fn [filter-text]
           (reset! current-filter filter-text))]
        [:div
         [rc/button
          :label "Add New Note"
          :on-click
          (fn []
            (reset! editing-note :new))]]
        [rc/h-box
         :children
         [[search-results current-filter]
          [active-tag-cloud]]]]])))
