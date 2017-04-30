(ns ^{:author "Daniel Leong"
      :doc "Exposed functions for templating"}
  sheater.templ.fun
  (:require [re-frame.core :refer [subscribe]])
  (:require-macros [sheater.templ.fun :refer [expose-fn]]))

(defn ^:export $->val
  "Read a static value"
  [k]
  (get
    (deref (subscribe [:active-static]))
    k))

(defn ^:export ->number
  [to-coerce]
  (when to-coerce
    (cond
      (number? to-coerce) to-coerce
      (not= -1 (.indexOf to-coerce ".")) (js/parseFloat to-coerce)
      :else (js/parseInt to-coerce))))

(defn ^:export $test
  [n]
  ($->val (keyword n)))

(defn ^:export mathify
  [args]
  (map ->number args))

(def exposed-fns
  (-> {;
       ; start with sheater APIs
       ;
       '$->val $->val
       }

      ;; (expose-fn $->val)

      ;;
      ;; Expose!
      ;;

      ;; (expose-math-fn +)
      ;; (expose-math-fn -)
      ;; (expose-math-fn /)
      ;; (expose-math-fn *)

      (expose-fn + mathify)
      (expose-fn - mathify)
      (expose-fn / mathify)
      (expose-fn * mathify)
      (expose-fn =)
      (expose-fn not=)

      (expose-fn keyword)
      (expose-fn name)
      (expose-fn str)
      (expose-fn symbol)
      (expose-fn vector)

      (expose-fn concat)
      (expose-fn cons)
      (expose-fn contains?)
      (expose-fn count)
      (expose-fn keys)
      (expose-fn vals)
      (expose-fn vec)

      (expose-fn get)
      (expose-fn get-in)

      (expose-fn filter)
      (expose-fn map)
      (expose-fn mapcat)
      (expose-fn remove)

      (expose-fn partial)))

;;
;; Public API
;;

(defn wrap-unknown-fn
  [sym]
  (let [sym-string (str sym)]
    (fn [& args]
      (js/console.warn
        "UNKNOWN or UNEXPOSED function: "
        sym-string)
      nil)))

(defn exposed-fn?
  [sym]
  (or (contains? exposed-fns sym)
      (not (nil? (->special-form sym)))))

(defn ->special-form
  [sym]
  (get
    {'let 'let*
     'fn 'fn*}
    sym))

(defn ^:export ->fun
  "Given a raw symbol, return the exposed function"
  [sym]
  (or (get exposed-fns sym)
      (->special-form sym)
      (wrap-unknown-fn sym)))

(when-not js/goog.DEBUG
  (js/goog.exportSymbol "cljs.core.Symbol"
                        cljs.core/Symbol)
  (js/goog.exportSymbol "cljs.core.Keyword"
                        cljs.core/Keyword)
  (js/goog.exportSymbol "cljs.core.PersistentArrayMap"
                        cljs.core/PersistentArrayMap)
  (js/goog.exportSymbol "cljs.core.PersistentHashMap"
                        cljs.core/PersistentHashMap)
  (js/goog.exportSymbol "cljs.core.PersistentHashSet"
                        cljs.core/PersistentHashSet)
  (js/goog.exportSymbol "cljs.core.PersistentVector"
                        cljs.core/PersistentVector))
