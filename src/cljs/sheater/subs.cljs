(ns ^{:author "Daniel Leong"
      :doc "Subscriptions"}
  sheater.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub :sheets :sheets)

(reg-sub
 :active-panel
 (fn [db _]
   (:active-panel db)))

(reg-sub
  :provider
  (fn [db [_ provider-id]]
    (-> db :providers provider-id)))
