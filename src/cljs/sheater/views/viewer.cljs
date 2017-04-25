(ns ^{:author "Daniel Leong"
      :doc "Sheet viewer"}
  sheater.views.viewer
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as rc]
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
       [[:nav.navbar.navbar-default
         [rc/h-box
          :children
          [[:div.navbar-header
            [:button.navbar-toggle.collapsed
             {:type "button"
              :data-toggle "collapse"
              :data-target ".navbar-responsive-collapse"}
             [:span.icon-bar]
             [:span.icon-bar]
             [:span.icon-bar]]
            [:div.navbar-brand
             {:href "#"}
             (:name info)]]
           [:div.navbar-collapse.collapse.navbar-responsive-collapse
            [:ul.nav.navbar-nav.nav-tabs
             (for [p (:pages data)]
               (let [url (str "#/sheets/" (name (:id info)) "/" (:name p))]
                 ^{:key (:name p)}
                 [:li
                  {:class (str "nav-link"
                               (when (= (:name p) page)
                                 " active"))}
                  [:a
                   {:href url
                    :on-click (fn [e]
                                (.preventDefault e)
                                (dispatch [:navigate-replace!  url]))}
                   (:name p) ]]))]
            ;
            [:ul.nav.navbar-nav.navbar-right
             [rc/hyperlink-href
              :label "Edit"
              :href (str "#/edit/"
                         (name (:id info))
                         "/"
                         page)]]]]]]
        [:div.container
         [render-page
         (->> data
              :pages
              (filter (comp (partial = page) :name))
              first)
         @(subscribe [:active-state])]]])]))

(defn panel
  [[id page]]
  (let [info @(subscribe [:sheet id])]
    (if info
      [render-sheet page info]
      [four-oh-four])))
