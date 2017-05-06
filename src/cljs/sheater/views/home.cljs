(ns ^{:author "Daniel Leong"
      :doc "Home page"}
  sheater.views.home
  (:require [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as rc]
            [sheater.provider :refer [providers]]
            [sheater.views.header :refer [header-bar]]))

(defn provider-picker
  []
  [:div.container.card
   [rc/v-box
    :children
    [(when @(subscribe [:sheets])
       [rc/hyperlink-href
        :label "Open a sheet"
        :href "#/sheets"])
     [rc/gap :size "2em"]
     [rc/title
      :label "Configure sheet provider:"
      :level :level4]
     [:ul
      (for [info (vals providers)]
        ^{:key (:id info)}
        [:li
         [rc/hyperlink-href
          :label (:name info)
          :href (str "#/provider/" (name (:id info)))]])]]]])

(defn panel
  []
  [rc/v-box
   :gap "1em"
   :children
   [[header-bar
     {:header "sheater"}]
    [provider-picker]]])
