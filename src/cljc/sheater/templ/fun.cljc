(ns ^{:author "Daniel Leong"
      :doc "Macros for exposing fns to templating"}
  sheater.templ.fun
  (:require [clojure.string :as str]))

(defmacro expose-fn
  [m fn-symbol & [run-on-args]]
  (let [n (name fn-symbol)
        this-ns-name (name (ns-name *ns*))
        exported-name (str "exported-" n)
        exported-symbol (symbol (str "exported-"
                                     (str/replace n #"/" "_SLASH_")))
        js-name (str this-ns-name "."
                     (str/replace
                       exported-name
                       #"[+-/*?]"
                       (fn [ch]
                         (case ch
                           "+" "_PLUS_"
                           "-" "_"
                           "/" "_SLASH_"
                           "*" "_STAR_"
                           "?" "_QMARK_"))))
        core-ns (-> fn-symbol resolve meta :ns ns-name name)
        core-ns-symbol (symbol core-ns n)]
    `(as-> ~m ~'m
       ;; (println ~(meta (resolve fn-symbol)))
       (do
         (defn ^:export ~exported-symbol
           [& ~'args]
           ~(if run-on-args
              `(apply ~core-ns-symbol (~run-on-args ~'args))
              `(apply ~core-ns-symbol ~'args)))
         (when-not js/goog.DEBUG
           (~'js/goog.exportSymbol ~js-name ~exported-symbol))
         (assoc ~'m (symbol ~n) (symbol
                                  ~this-ns-name
                                  ~(name exported-symbol)))))))

(defmacro expose-math-fn
  [m fn-symbol]
  `(expose-fn
     ~m
     ~fn-symbol
     sheater.templ.fun/mathify))

;; (expose-fn {} * sheater.templ.fun/mathify)

;; (defmacro expose-math
;;   [math-fn-symbol]
;;   )
