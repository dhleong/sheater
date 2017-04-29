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

      (expose-fn keyword)
      (expose-fn name)
      (expose-fn str)
      (expose-fn vector)

      (expose-fn concat)
      (expose-fn cons)
      (expose-fn contains?)
      (expose-fn keys)
      (expose-fn vals)
      (expose-fn vec)

      (expose-fn get)
      (expose-fn get-in)

      (expose-fn filter)
      (expose-fn map)
      (expose-fn mapcat)
      (expose-fn remove)))

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
  (contains? exposed-fns sym))

(defn ^:export ->fun
  "Given a raw symbol, return the exposed function"
  [sym]
  (or (get exposed-fns sym)
      (wrap-unknown-fn sym)))

