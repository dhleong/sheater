(ns ^{:author "Daniel Leong"
      :doc "Subscriptions"}
  sheater.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
 :active-panel
 (fn [db _]
   (:active-panel db)))

(reg-sub
  :provider
  (fn [db [_ provider-id]]
    (-> db :providers provider-id)))

(reg-sub
  :sheet
  (fn [db [_ id]]
    (-> db :sheets id)))

(reg-sub
  :sheets
  (fn [db]
    (->> db
         :sheets
         vals
         (sort-by :name))))

(reg-sub
  :sheet-page
  (fn [[_ sheet-id] _]
    (subscribe [:sheet sheet-id]))
  (fn [sheet, [_ _ page-id]]
    (->> sheet
         :data
         :pages
         (filter (comp (partial = page-id) :name))
         first)))
