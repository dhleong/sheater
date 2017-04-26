(ns ^{:author "Daniel Leong"
      :doc "Sheet (Template) Editor"}
  sheater.views.editor
  (:require [cljs.reader :as edn]
            [clojure.string :as str]
            [cljs.pprint :as pprint]
            [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as rc]
            [sheater.views.header :refer [action-button header-bar]]
            [sheater.views.viewer :as viewer]))

(defn prettify
  [markup]
  (with-out-str
    (pprint/write markup
                  :pretty true
                  :readably true
                  :right-margin 0)))

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

(defn parse-error
  [e]
  [rc/alert-box
   :alert-type :danger
   :body [:div
          [:h4 "Parse error"]
          (str e)]])

(defn render-page-editor
  "This complicated component renders an editor for the raw page template
  and a live preview of your changes, letting you know when there are
  errors in your input"
  [sheet-id page-id]
  (let [page (subscribe [:active-page])
        state (subscribe [:active-state])
        parse-error (reagent/atom nil)
        render-preview
        (fn [component]
          (let [preview-node (js/document.getElementById "page-preview")]
            (try
              (reagent/render
                (if-let [e @parse-error]
                  [parse-error e]
                  [viewer/render-page @page @state])
                preview-node)
              (catch :default e
                (js/console.error (.-stack e))
                ; NOTE: we would like to render like this,
                ; but it doesn't seem to work consistently...
                #_(reagent/render [parse-error e] preview-node)
                (set! (.-innerHTML preview-node)
                      (str e))))))]
    (reagent/create-class
      {:component-did-mount render-preview
       :component-did-update render-preview
       :display-name "render-page-editor"
       :reagent-render
       (fn [sheet-id page-id]
         (let [page @page
               _ @parse-error] ; deref to connect signal
           [rc/v-box
            :height "100%"
            :children
            [[:div#page-preview]
             [rc/gap :size "2em"]
             [rc/line]
             [rc/gap :size "1em"]
             [rc/input-textarea
              :model (prettify page)
              :class "editor"
              :width "50%"
              :rows 20
              :change-on-blur? false
              :on-change
              (fn [updated-page]
                (println "changed!")
                (try
                  (let [parsed (edn/read-string updated-page)]
                    (reset! parse-error nil)
                    (dispatch [:edit-sheet-page
                               sheet-id page-id
                               parsed]))
                  (catch :default e
                    (println "Not valid edn:" updated-page e)
                    (reset! parse-error e))))]]]))})))

(defn import-overlay
  [showing?]
  (let [whole-sheet @(subscribe [:active-sheet])
        sheet-id (:id whole-sheet)
        sheet-data (reagent/atom (:data whole-sheet))
        error (reagent/atom nil)
        include-state? (reagent/atom false)]
    (fn [showing?]
      [rc/modal-panel
       :backdrop-on-click #(reset! showing? false)
       :child
       [rc/v-box
        :gap "1em"
        :children
        [(when-let [err @error]
           [rc/alert-box
            :id :import-error
            :alert-type :danger
            :body (str err)])
         [rc/input-textarea
          :model (prettify
                   (if @include-state?
                     @sheet-data
                     (dissoc @sheet-data :state)))
          :rows 16
          :width "550px"
          :on-change
          (fn [data]
            (try
              (when-let [new-data (edn/read-string data)]
                (reset! error nil)
                (if @include-state?
                  (reset! sheet-data new-data)
                  (swap! sheet-data
                         (fn [old-sheet]
                           (assoc new-data
                                  :state (:state old-sheet))))))
              (catch :default e
                ; TODO notify
                (reset! error e)
                (js/console.warn "Error parsing import text" e))))]
         [rc/checkbox
          :label "Include user state (Careful! You could lose your state!)"
          :model include-state?
          :on-change (partial reset! include-state?)]
         [rc/h-box
          :justify :center
          :children
          [[rc/button
            :class "btn-raised btn-primary"
            :label "Import"
            :on-click
            (fn []
              (println "EDIT: " sheet-id @sheet-data)
              (dispatch [:edit-sheet sheet-id @sheet-data]))]]]]]])))

(defn render-editor
  [page info showing-import?]
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
         {:header
          [rc/hyperlink
           :label [:span
                   [:i.zmdi.zmdi-arrow-left]
                   [:span.navbar-collapse.collapse
                    (str " EDIT: " (:name info))]]
           :on-click (fn [e]
                       (.preventDefault e)
                       (println "heyyy")
                       (dispatch [:navigate!
                                  (str "#/sheets/" (name (:id info)) "/" page)])) ]
          :tabs
          (map
            (fn [p]
              {:url (str "#/edit/" (name (:id info)) "/" (:name p))
               :active? (= (:name p) page)
               :label (:name p)})
            (:pages data))
          :buttons
          [[action-button
            :label "Import"
            :on-click #(reset! showing-import? true)]
           [action-button
            :label "Save"
            :on-click (fn []
                        (dispatch
                          [:save-sheet!
                           (:id info)]))]]}]
        (when @showing-import?
          [import-overlay showing-import?])
        [:div.container
         [render-page-editor (:id info) page]]])]))

(defn panel
  [[id page]]
  (let [showing-import? (reagent/atom false)]
    (fn [[id page]]
      (let [info @(subscribe [:sheet id])]
        (if info
          [render-editor page info showing-import?]
          [four-oh-four])))))
