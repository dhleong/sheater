(ns ^{:author "Daniel Leong"
      :doc "Sheets list view"}
  sheater.views.sheets
  (:require [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as rc]))

(defn title []
  [rc/title
   :label "Sheater"
   :level :level1])

(defn panel
  []
  (let [sheets @(subscribe [:sheets])]
    (println "sheets=" sheets)
    [rc/v-box
     :height "100%"
     :children
     [[title]
      (if-not (seq sheets)
        [:div "No sheets created. "
         [rc/hyperlink-href
          :label "Create one now!"
          :href "#/sheet/create"]]
        [rc/hyperlink-href
          :label "New sheet"
          :href "#/sheet/create"])
      (when (seq sheets)
        [:ul
         (for [sheet sheets]
           (do (println "sheet=" sheet)
               ^{:key (:id sheet)}
               [:li
                [rc/hyperlink-href
                 :label (:name sheet)
                 :href (str "#/sheets/" (:id sheet))]]))])
      ]]))
