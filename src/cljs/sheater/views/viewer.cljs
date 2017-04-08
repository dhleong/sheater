(ns ^{:author "Daniel Leong"
      :doc "Sheet viewer"}
  sheater.views.viewer
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
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
  [page info]
  (let [data (:data info)]
    (when-not data
      ; don't got it? get it!
      (dispatch [:refresh! (:id info)]))
    (when (and data
               (nil? @page))
      (reset! page (-> data :pages first :name)))
    [rc/v-box
     :height "100%"
     :children
     (if-not data
       [[rc/throbber
         :size :large]]
       ;
       [[rc/h-box
         :gap "2em"
         :children
         [[rc/title
           :label (:name info)
           :level :level3]
          ;
          [rc/horizontal-tabs
           :tabs (->> data :pages
                      (map (fn [page]
                             {:label (:name page)
                              :id (:name page)})))
           :model page
           :on-change (fn [id]
                        (reset! page id))]]]
       [:div "TODO:" info]
       ])]))

(defn panel
  [id]
  (let [info @(subscribe [:sheet id])
        page (reagent/atom nil)]
    (if info
      [render-sheet page info]
      [four-oh-four])))
