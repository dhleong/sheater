(ns ^{:author "Daniel Leong"
      :doc "custom-page"}
  sheater.views.pages.custom-page
  (:require [cljs.js :refer [empty-state eval js-eval]]
            [re-com.core :as rc]
            [re-frame.core :refer [subscribe dispatch]]))

(defonce state (atom nil))

(defn write-state
  [k v]
  (println k " <- " v)
  (dispatch [:edit-sheet-state k v]))

(defn eval-form
  [form]
  (let [compiler-state
        (if-let [cached-state @state]
          cached-state
          (let [new-state (empty-state)]
            (reset! state new-state)))]
    (eval compiler-state
          form
          {:eval       js-eval
           :analyze-deps false
           :source-map true
           :context    :expr
           :ns 'sheater.views.pages.custom-page}
          :value)))

(declare translate)

(defn wrap-with-rc
  [page state fun children]
  [fun
   :children
   (vec (map (partial translate page state) children))])

(defn inflate-value-fn-key
  [page state kw]
  (let [n (name kw)]
    (case (first n)
      \$ (let [static-key (keyword (subs n 1))]
           `(deref (subscribe [:static
                               ~(:name page)
                               ~static-key])))
      \# (let [data-key (keyword (subs n 1))]
           (get state data-key))
      ; normal keyword
      kw)))

(defn inflate-value-fn-part
  [page state part]
  (cond
    (keyword? part) (inflate-value-fn-key page state part)
    (seq? part) (map (partial inflate-value-fn-part page state) part)
    (vector? part) (vec (map (partial inflate-value-fn-part page state) part))
    :else part))

(defn inflate-value-fn
  [page state {:keys [symbols?]} fun]
  ; TODO
  (println "INFLATE FUN:" fun)
  (let [form (inflate-value-fn-part page state fun)]
    (println "->" form)
    form))

(defn flatten-vec
  [to-vec]
  (vec
    (mapcat
      (fn [item]
        (if (and (seq? item)
                 (vector? (first item)))
          item
          [item]))
      to-vec)))

(defn inflate-for
  [page state element]
  (let [[_ bindings body] element
        bindings (vec (map (partial inflate-value-fn-part page state) bindings))
        body (translate page state {:symbols? true} body)]
    ;; (cljs.pprint/pprint bindings)
    (let [evald (eval-form
                  `(for ~bindings
                     ~body))]
      ;; (cljs.pprint/pprint evald)
      evald)))

(defn translate-auto-input
  "The last arg is guaranteed to be a vector"
  [page state {:keys [symbols?] :as opts} [kind arg]]
  (when (keyword? kind)
    (let [n (name kind)
          id-sep (.indexOf n "#")
          has-auto-id? (not= id-sep -1)]
      (when (or has-auto-id?
                (:id arg))
        (let [id (if has-auto-id?
                   (keyword (subs n (inc id-sep)))
                   (:id arg))
              value (get state id "")]
          ;; (println "TRANSLATE" symbols? kind id)
          (if symbols?
            `[rc/input-text
              :model (str @(subscribe [:active-state ~id]))
              :on-change (partial write-state ~id)]
            [rc/input-text
             :model value
             :on-change (partial write-state id)]))))))

;; TODO do this just once and cache the result.
;; Otherwise, switching pages on even a moderately
;; complicated sheet is going to be sloooow.
(defn translate
  ([page state element]
   (translate page state {} element))
  ([page state {:keys [symbols?] :as opts} element]
   (cond
     (vector? element)
     (let [kind (first element)]
       (case kind
         :rows (wrap-with-rc
                 page
                 state
                 rc/v-box
                 (rest element))
         :cols (wrap-with-rc
                 page
                 state
                 rc/h-box
                 (rest element))
         :checkbox (let [v (second element)]
                     [(if symbols? 're-com.core/checkbox rc/checkbox)
                      :model (inflate-value-fn page state opts (:checked v))
                      :on-change (if symbols? 'identity identity)
                      :disabled? true])
         (if-let [auto-input (translate-auto-input page state opts element)]
           ; it was eg: :input#name
           auto-input
           ; leave it alone, but translate its kids
           (if-let [kids (seq (rest element))]
             (flatten-vec
               (cons kind
                     (map (partial translate page state opts) kids)))
             element))))
     (= 'for (first element)) (inflate-for page state element)
     :else element)))

(defn render
  [page state]
  [:div
   (let [tr (translate page state (:spec page))]
     ;; (cljs.pprint/pprint tr)
     tr)])
