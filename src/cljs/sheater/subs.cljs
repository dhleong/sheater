(ns ^{:author "Daniel Leong"
      :doc "Subscriptions"}
  sheater.subs
  (:require [re-frame.core :refer [reg-sub trim-v subscribe]]))

(reg-sub
 :name
 (fn [db]
   (:name db)))

(reg-sub
 :active-panel
 (fn [db _]
   (:active-panel db)))
