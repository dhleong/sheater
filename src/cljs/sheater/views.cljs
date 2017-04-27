(ns ^{:author "Daniel Leong"
      :doc "Views switcher"}
  sheater.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as rc]
            [sheater.views.create :as create]
            [sheater.views.editor :as editor]
            [sheater.views.home :as home]
            [sheater.views.provider :as provider]
            [sheater.views.sheets :as sheets]
            [sheater.views.viewer :as viewer]))

;; main

(defn- panels [panel-name & [args]]
  (case panel-name
    :home-panel [home/panel]
    :provider-panel [provider/panel args]
    :sheets [sheets/panel]
    :sheet/create [create/panel]
    :viewer [viewer/panel args]
    :editor [editor/panel args]
    [:div "Oops! Unknown panel:" panel-name]))

(defn main-panel []
  (let [active-panel (subscribe [:active-panel])]
    (fn []
      (let [[panel args] @active-panel]
        [rc/v-box
         :height "100%"
         :children [[panels panel args]]]))))
