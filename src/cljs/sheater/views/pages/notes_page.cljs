(ns ^{:author "Daniel Leong"
      :doc "Notes page"}
  sheater.views.pages.notes-page
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as rc]
            [sheater.widgets :as widg]))

(def tag-regex #"(#([a-zA-Z0-9_-]+))")

(defn ->query
  [filter-atom]
  (reagent/cursor filter-atom [:query]))

(defn extract-tags
  [text]
  (map
    last
    (re-seq tag-regex text)))

(defn tag-cloud
  ([filter-text-atom tags]
   [:div.tag-cloud
    (for [t tags]
      ^{:key t}
      [:div.tag-item
       [rc/hyperlink
        :label (str "#" t)
        :on-click #(reset! filter-text-atom t)]])])
  ([tags]
   [:div.tag-cloud
    (for [t tags]
      ^{:key t}
      [:div.tag-item (str "#" t)])]))

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
          :attr {:auto-focus true}
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
            (dispatch [:update-active-note! @note]))]]]])))

(defn active-tag-cloud
  [filter-atom]
  (let [tags @(subscribe [:active-note-tags])
        filter-text (->query filter-atom)]
    [rc/box
     :child
     [tag-cloud filter-text tags]]))

(defn note-card
  [filter-atom editing-note note]
  (let [mouse-over? (reagent/atom false)
        filter-text (->query filter-atom)]
    (fn [_ editing-note note]
      [:div.card
       {:on-mouse-over #(reset! mouse-over? true)
        :on-mouse-out #(reset! mouse-over? false)}
       [rc/v-box
        :class "card-contents"
        :children
        [(:body note)
         [tag-cloud filter-text (:tags note)]]]
       [rc/h-box
        :class (str "card-buttons"
                    (when (empty? (:tags note))
                      " single"))
        :gap "1em"
        :children
        [[rc/row-button
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
     :children
     (for [n notes]
       ^{:key (:created n)}
       [note-card filter-atom editing-note n])]))

(defn sort-options
  [sort-setting]
  (let [current-sort @sort-setting]
    [rc/h-box
     :align :center
     :gap ".5em"
     :height "100%"
     :justify :end
     :children
     [[rc/button
       :label
       [:span
        "Sort "
        [:i.zmdi
         {:class (case current-sort
                   :created-asc "zmdi-sort-amount-asc"
                   :created-desc "zmdi-sort-amount-desc")}]]
       :on-click (fn []
                   (reset! sort-setting
                           (case current-sort
                             :created-asc :created-desc
                             :created-desc :created-asc)))]]]))

(defn render
  [page state]
  (let [current-filter (reagent/atom {:query ""
                                      :sorting :created-desc})
        filter-text (->query current-filter)
        filter-sort (reagent/cursor current-filter [:sorting])
        editing-note (reagent/atom nil)]
    (fn []
      [rc/v-box
       :gap "1em"
       :children
       [(when-let [editing @editing-note]
          [edit-note-dialog editing-note editing])
        [:div.row.search-row
         [:div.col-md-5.col-sm-12
          [rc/h-box
           :style {:width "100%"}
           :class "search-bar"
           :gap "1em"
           :children
           [[rc/button
             :class "btn-raised btn-primary"
             :label "Add New Note"
             :on-click
             (fn []
               (reset! editing-note :new))]
            [widg/clearable-input-text
             :attr {:id "notes-search"}
             :class "search"
             :width "auto"
             :model filter-text
             :placeholder "Search"
             :change-on-blur? false
             :on-change
             (partial reset! filter-text)]]]]
         [:div.col-md-3.col-sm-12.filters
          [sort-options filter-sort]]]
        [:div.row
         [:div.col-md-4.col-md-push-8
          [active-tag-cloud current-filter]]
         [:div.col-md-8.col-md-pull-4
          [search-results editing-note current-filter]]]]])))
