(ns ^{:author "Daniel Leong"
      :doc "Notes page"}
  sheater.views.pages.notes-page
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as rc]))

(defn render
  [state]
  [:div "NOTES!"])
