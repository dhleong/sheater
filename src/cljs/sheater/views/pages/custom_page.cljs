(ns ^{:author "Daniel Leong"
      :doc "custom-page"}
  sheater.views.pages.custom-page
  (:require [clojure.string :as str]
            [cljs.js :refer [empty-state eval js-eval]]
            [reagent.impl.template :refer [parse-tag]]
            [re-com.core :as rc]
            [re-frame.core :refer [subscribe dispatch]]
            [sheater.views.pages.custom-page.widgets :as widg]
            [sheater.templ.fun :refer [exposed-fn? $->val ->fun ->number]]))

(declare translate)

(defn- ->js [var-name]
  (-> var-name
      (str/replace #"/" ".")
      (str/replace #"-" "_")))

(def widget-types
  (->> [[:checkbox `widg/checkbox]
        [:currency `widg/currency]
        [:input `widg/input]
        [:input-calc `widg/input-calc]
        [:inventory `widg/inventory]
        [:dynamic-table `widg/dynamic-table]
        [:partial-number `widg/partial-number]
        [:picker `widg/picker]
        [:selectable-list `widg/selectable-list]
        [:selectable-set `widg/selectable-set]]

       ; dynamically generate the mapping once
       ; to avoid a lot of yuck. The resulting map
       ; looks like
       ;  {:key {:symbol widg/symbol :fn actual-fn}
       (map
         (fn [[k sym]]
           [k {:symbol sym
               :fn (js/eval (->js (str sym)))}]))
       (into {})))

;;
;; Clojurescript eval

(def cached-eval-state (atom nil))

(defn- eval-in
  [state form]
  (eval state
        form
        {:eval (fn [src]
                 (try
                   (js-eval src)
                   (catch :default e
                     (js/console.warn "FAILED to js/eval:" (:source src))
                     (throw e))))
         :context :expr
         :source-map true
         :ns 'sheater.views.pages.custom-page}
        :value))

(defn eval-form
  [form]
  (let [compiler-state
        (if-let [cached @cached-eval-state]
          cached
          (let [new-state (empty-state)]
            ;
            ; eval an ns so the imports are recognized
            (eval-in
              new-state
              '(ns sheater.views.pages.custom-page
                 (:require [re-frame.core :as re]
                           [re-com.core :as rc]
                           [sheater.views.pages.custom-page.widgets :as widg]
                           [sheater.templ.fun])))
            ;
            ; eval a declare so our functions are also recognized
            #_(eval-in
              new-state
              '(do
                 (declare sheater.templ.fun/exported-keys
                          sheater.templ.fun/$->val)))
            #_(eval-in
              new-state
              '(declare-all-exposed))
            (reset! cached-eval-state new-state)))]
    (try
      (eval-in compiler-state
               form)
      (catch :default e
        (js/console.error "Error compiling:" (str form), e)
        (throw e)))))

;;
;; Custom form translation/inflation

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
           ; just get from :active-static so they all share
           ; a single subscription
           `($->val
              ; pass through (inflate) again in case we need to
              ; use the exposed- factory
              ~(inflate-value-fn-key page state static-key)))
      \# (let [data-key (keyword (subs n 1))]
           (get state data-key))
      ; normal keyword;
      (if js/goog.DEBUG
        kw
        ; under advanced compilation, the cljs.core.Keyword constructor
        ; disappears, so we have to call through to the exposed factory
        `(sheater.templ.fun/exported-keyword ~n)))))

(defn inflate-value-fn-part
  [page state part]
  (cond
    (keyword? part) (inflate-value-fn-key page state part)
    (seq? part) (map (partial inflate-value-fn-part page state) part)
    (vector? part) `(sheater.templ.fun/exported-vector ~@(map (partial inflate-value-fn-part page state) part))
    (exposed-fn? part) (->fun part)
    :else part))

(defn inflate-value-fn
  [page state {:keys [symbols?]} fun]
  ;; (println "INFLATE FUN:" fun)
  (let [form (inflate-value-fn-part page state fun)]
    ;; (println "->" form)
    (if symbols?
      form
      (eval-form form))))

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

(defn destructure-auto-input
  [[kind arg & children]]
  (when (keyword? kind)
    (let [parsed (parse-tag kind)
          name (.-name parsed)
          id (or (.-id parsed)
                 (:id arg))
          class (or (.-className parsed)
                    (:class arg))]
      (when (or id
                (:value arg))
        (vec
          (concat
            [(keyword name)
             (assoc arg
                    :id (if (string? id)
                          (keyword id)
                          id)
                    :class class)]
            children))))))

(defn translate-auto-input
  "`element` is guaranteed to be a vector"
  [page state {:keys [symbols?] :as opts} element]
  (when-let [[el arg children] (destructure-auto-input element)]
    (let [arg (if (:items arg)
                (update arg :items
                        (partial inflate-value-fn page state opts))
                arg)
          arg (if (:value arg)
                (update arg :value
                        (partial inflate-value-fn page state opts))
                arg)]
      ; finally, let the right one in:
      (if-let [factory (get widget-types el)]
        (if symbols?
          `[~(:symbol factory) ~arg ~children]
          (vec
            (concat [(:fn factory) arg]
                    children)))
        [:div "Unknown element type" el]))))

;; TODO do this just once and cache the result.
;; Otherwise, switching pages on even a moderately
;; complicated sheet is going to be sloooow.
(defn ^:export translate
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
         :cols [widg/cols
                (map (partial translate page state)
                     (rest element)) ]
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
     (seq? element) (inflate-value-fn page state opts element)
     :else element)))

;;
;; Main render

(defn render
  [page state]
  [:div
   (let [tr (translate page state (:spec page))]
     ;; (cljs.pprint/pprint tr)
     tr)])
