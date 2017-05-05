(ns ^{:author "Daniel Leong"
      :doc "General widgets"}
  sheater.widgets
  (:require [re-com.core :as rc]))

(defn clearable-input-text
  "Drop-in replacement for rc/input-text that provides
   a button to clear the input. If you provide an :id
   in the :attr key, we will focus on the input field
   after clearing its contents from the button"
  [& {:keys [model on-clear attr] :as args}]
  (let [atom? (satisfies? IDeref model)
        value (if atom?
                (deref model)
                model)
        on-clear (or on-clear
                     (when atom?
                       (fn []
                         (reset! model ""))))
        wrapped-on-clear (if-let [id (:id attr)]
                           (fn []
                             (on-clear)
                             (.focus (js/document.getElementById id)))
                           on-clear)]
    [:div.clearable-input.display-flex
     (into [rc/input-text]
           (apply concat args))
     (when-not (empty? value)
       [rc/md-icon-button
        :class "clear-button"
        :md-icon-name "zmdi-close"
        :on-click wrapped-on-clear])]))
