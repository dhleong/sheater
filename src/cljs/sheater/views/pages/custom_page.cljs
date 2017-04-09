(ns ^{:author "Daniel Leong"
      :doc "custom-page"}
  sheater.views.pages.custom-page
  (:require [re-com.core :as rc]))

(declare translate)

(defn wrap-with-rc
  [fun children]
  [fun
   :children
   (vec (map translate children))])

(defn translate
  [element]
  (if (vector? element)
    (let [kind (first element)]
      (case kind
        :rows (wrap-with-rc
                rc/v-box
                (rest element))
        :cols (wrap-with-rc
                rc/h-box
                (rest element))
        ; leave it alone
        element))
    element))

(defn render
  [page state]
  (let [spec (:spec page)]
    [:div
     (translate spec)]))
