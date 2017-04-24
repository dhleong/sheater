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
  ([filter-atom tags]
   [:div.tag-cloud
    (for [t tags]
      ^{:key t}
      [:div.tag
       [rc/hyperlink
        :label (str "#" t)
        :on-click #(reset! filter-atom t)]])])
  ([tags]
   [:div.tag-cloud
    (for [t tags]
      ^{:key t}
      [:div.tag (str "#" t)])]))

(defn edit-note-dialog
  [showing? note-created]
  (let [note (reagent/atom {:body ""
                            :created (js/Date.now)
                            :tags []})]
    (when-not (or (nil? note-created)
                  (= :new note-created))
      (let [notes @(subscribe [:active-notes])
            existing (->> notes
                          (filter
                            (comp (partial = note-created)
                                  :created))
                          first)]
        (when existing
          (println "Found existing: " existing)
          (reset! note existing))))
    (fn [showing? note-created]
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
          :rows 6
          :width "350px"
          :on-change
          (fn [body]
            (swap! note assoc
                   :body body
                   :tags (into #{} (extract-tags body))))]
         [tag-cloud (:tags @note)]
         [rc/button
          :label (if (not= :new note-created)
                   "Update"
                   "Add")
          :on-click
          (fn [e]
            (.preventDefault e)
            (reset! showing? false)
            (println "update-active-note!" @note)
            (dispatch [:update-active-note! @note]))]]]])))

(defn active-tag-cloud
  [filter-atom]
  (let [tags @(subscribe [:active-note-tags])]
    [rc/box
     :width "40%"
     :child
     [tag-cloud filter-atom tags]]))

(defn note-card
  [filter-atom editing-note note]
  (let [mouse-over? (reagent/atom false)]
    (fn [filter-atom editing-note note]
      [:div.card
       {:on-mouse-over #(reset! mouse-over? true)
        :on-mouse-out #(reset! mouse-over? false)}
       [rc/h-box
        :gap ".5em"
        :children
        [[:div.card
          (:body note)
          [tag-cloud filter-atom (:tags note)]]
         [rc/row-button
          :mouse-over-row? @mouse-over?
          :md-icon-name "zmdi-edit"
          :on-click
          (fn []
            (reset! editing-note (:created note)))]
         [rc/row-button
          :mouse-over-row? @mouse-over?
          :md-icon-name "zmdi-delete"
          :on-click #(dispatch [:delete-active-note! note])]]]])))

(defn search-results
  [editing-note filter-atom]
  (let [notes @(subscribe [:search-notes @filter-atom])]
    [rc/v-box
     :width "60%"
     :children
     (for [n notes]
       ^{:key (:created n)}
       [note-card filter-atom editing-note n])]))

(defn render
  [page state]
  (let [current-filter (reagent/atom "")
        editing-note (reagent/atom nil)]
    (fn []
      [rc/v-box
       :gap "1em"
       :children
       [(when-let [editing @editing-note]
          [edit-note-dialog editing-note editing])
        [rc/h-box
         :gap "1em"
         :children
         [[rc/button
           :label "Add New Note"
           :on-click
           (fn []
             (reset! editing-note :new))]
          [rc/input-text
           :model current-filter
           :placeholder "Search"
           :change-on-blur? false
           :on-change
           (fn [filter-text]
             (reset! current-filter filter-text))]]]
        [rc/h-box
         :children
         [[search-results editing-note current-filter]
          [active-tag-cloud current-filter]]]]])))
