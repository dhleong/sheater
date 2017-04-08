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

(defn sheet-by-id
  [db id]
  (->> db
       :sheets
       (filter (comp (partial = id) :id))
       first))

(reg-sub
  :sheet
  (fn [db [_ id]]
    (sheet-by-id db id)))
