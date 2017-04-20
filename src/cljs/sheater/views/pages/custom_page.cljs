(ns ^{:author "Daniel Leong"
      :doc "custom-page"}
  sheater.views.pages.custom-page
  (:require [re-com.core :as rc]))

(declare translate)

(defn wrap-with-rc
  [spec state fun children]
  [fun
   :children
   (vec (map (partial translate spec state) children))])

(defn inflate-value-fn
  [spec state fun]
  ; TODO
  (println "INFLATE FUN:" fun)
  true)

(defn translate
  [spec state element]
  (if (vector? element)
    (let [kind (first element)]
      (case kind
        :rows (wrap-with-rc
                spec
                state
                rc/v-box
                (rest element))
        :cols (wrap-with-rc
                spec
                state
                rc/h-box
                (rest element))
        :checkbox (let [v (second element)]
                    [rc/checkbox
                     :model (inflate-value-fn spec state (:checked v))
                     :on-change (fn [_] nil)
                     :disabled? true])
        ; leave it alone, but translate its kids
        (if-let [kids (seq (rest element))]
          (vec (cons kind
                     (map (partial translate spec state) kids)))
          element)))
    element))

(defn render
  [page state]
  (let [spec (:spec page)]
    [:div
     (translate spec state spec)]))
