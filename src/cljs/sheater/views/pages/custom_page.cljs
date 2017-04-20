(ns ^{:author "Daniel Leong"
      :doc "custom-page"}
  sheater.views.pages.custom-page
  (:require [re-com.core :as rc]
            [cljs.js :refer [empty-state eval js-eval]]))

(defn compile-code
  [form]
  (eval (empty-state)
        form
        {:eval       js-eval
         :source-map true
         :context    :expr}
        :value))

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

(defn flatten-vec
  [to-vec]
  (vec
    (mapcat
      (fn [item]
        (if (seq? item)
          item
          [item]))
      to-vec)))

(defn translate
  [spec state element]
  (cond
    (vector? element)
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
          (flatten-vec
            (cons kind
                  (map (partial translate spec state) kids)))
          element)))
    (= 'for (first element))
    (let [[_ bindings body] element]
      (map (partial translate spec state)
           (compile-code
             `(for ~bindings ~body))))
    :else element))

(defn render
  [page state]
  (let [spec (:spec page)]
    [:div
     (let [tr (translate spec state spec)]
       (cljs.pprint/pprint tr)
       tr)]))
