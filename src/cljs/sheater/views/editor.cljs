(ns ^{:author "Daniel Leong"
      :doc "Sheet (Template) Editor"}
  sheater.views.editor
  (:require [cljs.reader :as edn]
            [clojure.string :as str]
            [cljs.pprint :as pprint]
            [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as rc]
            [sheater.views.header :refer [header-bar]]
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

(defn render-editor
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
       ; TODO: component:
       [[header-bar
         {:header (str "EDIT: " (:name info))
          :tabs
          (map
            (fn [p]
              {:url (str "#/edit/" (name (:id info)) "/" (:name p))
               :active? (= (:name p) page)
               :label (:name p)})
            (:pages data))
          :buttons
          [[rc/md-circle-icon-button
            :md-icon-name "zmdi-floppy"
            :tooltip "Save"
            :on-click (fn []
                        (dispatch
                          [:save-sheet!
                           (:id info)]))]]}]
        [:div.container
         [render-page-editor (:id info) page]]])]))

(defn panel
  [[id page]]
  (let [info @(subscribe [:sheet id])]
    (if info
      [render-editor page info]
      [four-oh-four])))
