(ns sheater.views
    (:require [re-frame.core :refer [subscribe dispatch]]
              [re-com.core :as rc]
              [sheater.provider :refer [providers]]
              [sheater.views.create :as create]
              [sheater.views.sheets :as sheets]
              [sheater.views.viewer :as viewer]))

;; home

(defn home-title []
  [rc/title
   :label "sheater"
   :level :level1])

(defn provider-picker []
  [:ul
   (for [info (vals providers)]
     ^{:key (:id info)}
     [:li
      [rc/hyperlink-href
       :label (:name info)
       :href (str "#/provider/" (name (:id info)))]])])

(defn home-panel []
  [rc/v-box
   :gap "1em"
   :children [[home-title]
              [provider-picker]]])


;; about

(defn about-title []
  [rc/title
   :label "This is the About Page."
   :level :level1])

(defn link-to-home-page []
  [rc/hyperlink-href
   :label "go to Home Page"
   :href "#/"])

(defn about-panel []
  [rc/v-box
   :gap "1em"
   :children [[about-title] [link-to-home-page]]])


;; main

(defn- panels [panel-name & [args]]
  (case panel-name
    :home-panel [home-panel]
    :about-panel [about-panel]
    :provider-panel [(-> providers args :panel)]
    :sheets [sheets/panel]
    :sheet/create [create/panel]
    :viewer [viewer/panel args]
    [:div "Oops!"]))

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (subscribe [:active-panel])]
    (fn []
      (let [[panel args] @active-panel]
        [rc/v-box
         :height "100%"
         :children [[panels panel args]]]))))
