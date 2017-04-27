(ns ^{:author "Daniel Leong"
      :doc "Provider view facade"}
  sheater.views.provider
  (:require [re-com.core :as rc]
            [sheater.provider :refer [providers]]
            [sheater.views.header :refer [header-bar]]))

(defn panel
  [provider-id]
  (let [provider (get providers provider-id)]
    (println (:panel provider))
    [rc/v-box
     :height "100%"
     :children
     [[header-bar
       {:header (:name provider)}]
      [:div.container
       [(:panel provider)]]]]))
