(ns ^{:author "Daniel Leong"
      :doc "custom-page"}
  sheater.views.pages.custom-page
  (:require [clojure.string :as str]
            [cljs.js :refer [empty-state eval js-eval]]
            [cljs.reader :as edn]
            [reagent.impl.template :refer [parse-tag]]
            [re-com.core :as rc]
            [re-frame.core :refer [subscribe dispatch]]
            [sheater.views.pages.custom-page.widgets :as widg]
            [sheater.templ.fun :refer [exposed-fn? $->val state->val ->fun ->number]]))

(declare translate inflate-value-fn)
(def special-let-forms
  #{'let 'fn 'let* 'fn*
    'when-let 'if-let})

(defn- ->js [var-name]
  (-> var-name
      (str/replace #"/" ".")
      (str/replace #"-" "_")))

(def widget-types
  (->> [[:checkbox `widg/checkbox]
        [:consumables `widg/consumables]
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

(defn process-source
  [js-src]
  (-> js-src
      (str/replace
        ; we could also just replace the _invoke sequence, but
        ; that may or may not be safe....
        #"(new cljs\.core\.Keyword\(null,\"[^\"]+\",\"[^\"]+\",\([0-9-]+\)\))\.cljs\$core\$IFn\$_invoke\$arity\$([0-9]+)\("
        "$1.call(null,")))

(defn- eval-in
  [state form]
  ;; (js/console.warn "(eval-in): " (str form))
  (eval state
        form
        {:eval (fn [src]
                 (let [src (update src :source process-source)]
                   (try
                     ;; (js/console.warn "(js/eval): " (:source src))
                     (js-eval src)
                     (catch :default e
                       (js/console.warn "FAILED to js/eval:" (:source src))
                       (throw e)))))
         :context :expr
         :source-map true
         :ns 'sheater.views.pages.custom-page}
        (fn [res]
          (if (contains? res :value) ; nil or false are fine
            (:value res)
            (when-not (= "Could not require re-frame.core"
                         (ex-message (:error res)))
              (js/console.error (str "Error evaluating: " form))
              (js/console.error (str res)))))))

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
            (reset! cached-eval-state new-state)))]
    (try
      (eval-in compiler-state
               form)
      (catch :default e
        (js/console.error "Error compiling:" (str form), e)
        (throw e)))))

(defn ^:export eval-fn-string
  ([string]
   (eval-fn-string false string))
  ([symbols? string]
   (inflate-value-fn
     @(subscribe [:active-page])
     @(subscribe [:active-state])
     {:symbols? symbols?}
     (edn/read-string string))))

(defn ^:export eval-string
  [string]
  (eval-form
    (translate
      @(subscribe [:active-page])
      @(subscribe [:active-state])
      (edn/read-string string))))

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
           `($->val ~static-key))
      \# (let [data-key (keyword (subs n 1))]
           ; TODO can we ever just use the state?
           ; does it matter, perf-wise? If not,
           ; should we just stop passing along `state`?
           `(state->val ~data-key))
      ; normal keyword;
      kw)))

(declare inflate-value-fn-part)
(defn inflate-value-fn-seq
  [page state part]
  ; handle some special forms
  (let [kind (first part)]
    (if (contains? special-let-forms kind)
      (concat [(->fun kind) (vec (inflate-value-fn-seq page state (second part)))]
              ; body:
              (inflate-value-fn-seq page state
                                    (drop 2 part)))
      (map (partial inflate-value-fn-part page state) part))))

(defn inflate-value-fn-part
  [page state part]
  (cond
    (keyword? part) (inflate-value-fn-key page state part)
    (seq? part) (inflate-value-fn-seq page state part)
    (vector? part) (vec (map (partial inflate-value-fn-part page state) part))
    (exposed-fn? part) (->fun part)
    :else part))

(defn inflate-value-fn
  [page state {:keys [symbols? call?]} fun]
  ;; (js/console.log "INFLATE FUN:" (str fun))
  (let [form (inflate-value-fn-part page state fun)
        form (if call?
               (concat ['fn []]
                       [form])
               form)]
    ;; (js/console.log (when symbols? "symbols") "->" (str form))
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
    (when (> (count bindings) 2)
      (js/console.warn "(for ", (str bindings), " ...)", (count bindings))
      (cljs.pprint/pprint bindings))
    (let [evald (eval-form
                  #_(for ~bindings
                     ~body)
                  `(sheater.templ.fun/exported-map
                     (fn* [~(first bindings)]
                          ~body)
                     ~(second bindings)))]
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

(defn update-if
  [m k fun]
  (if (k m)
    (update m k fun)
    m))

(defn translate-auto-input
  "`element` is guaranteed to be a vector"
  [page state {:keys [symbols?] :as opts} element]
  (when-let [[el arg children] (destructure-auto-input element)]
    (let [inflate-arg-part (partial inflate-value-fn page state opts)
          inflate-callable-part (partial inflate-value-fn page state
                                         (assoc opts
                                                :call? true))
          arg (-> arg
                  (update-if :id inflate-arg-part)
                  (update-if :items inflate-arg-part)
                  (update-if :value inflate-callable-part))]
      ; finally, let the right one in:
      (let [result  (if-let [factory (get widget-types el)]
                      (if symbols?
                        `[~(:symbol factory) ~arg ~children]
                        (vec
                          (concat [(:fn factory) arg]
                                  children)))
                      [:div "Unknown element type" el])]
        ;; (println "Translated " element " - > " result)
        result))))

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
