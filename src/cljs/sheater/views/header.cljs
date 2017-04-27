(ns ^{:author "Daniel Leong"
      :doc "Shared header/action bar component"}
  sheater.views.header
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as rc]))

(defn header-bar
  [opts]
  [:nav.navbar.navbar-default
   [:div.container
    [:div.navbar-header
     [:div.navbar-brand
      (:header opts)]]
    (when-let [tabs (:tabs opts)]
      [:ul.nav.navbar-nav.nav-tabs
       (for [t tabs]
         ^{:key (:label t)}
         [:li
          {:class (str "nav-link"
                       (when (:active? t)
                         " active"))}
          [:a
           {:href (:url t)
            :on-click (fn [e]
                        (.preventDefault e)
                        (dispatch [:navigate-replace! (:url t)]))}
           (:label t)]])])
    [:button.navbar-toggle.collapsed
     {:type "button"
      :data-toggle "collapse"
      :data-target ".navbar-responsive-collapse"}
     [rc/md-icon-button
      :md-icon-name "zmdi-more-vert"
      :on-click identity]]
    [:div.navbar-collapse.collapse.navbar-responsive-collapse
     ;
     (when-let [buttons (:buttons opts)]
       (vec
         (cons
           :ul.nav.navbar-nav.navbar-right
           (map
             (fn [b]
               [:li b])
             buttons))))]]])

(defn action-button
  "Drop-in replacement for rc/hyperlink for use in the header-bar"
  [& {:keys [on-click label]}]
  [:a ; can't use rc/hyperlink here because it breaks styles :/
   {:href "#"
    :on-click
    (fn [e]
      (.preventDefault e)
      (on-click))}
   label])