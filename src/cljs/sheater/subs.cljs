(ns ^{:author "Daniel Leong"
      :doc "Subscriptions"}
  sheater.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub :sheets-map :sheets)

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

(reg-sub
  :active-sheet-id
  :<- [:active-panel]
  (fn [[panel arg] _]
    (when (or (= :viewer panel)
              (= :editor panel))
      arg)))

(reg-sub
  :active-sheet
  :<- [:sheets-map]
  :<- [:active-sheet-id]
  (fn [[sheets sheet-id] _]
    (get sheets sheet-id)))

(reg-sub
  :active-data
  :<- [:active-sheet]
  (fn [sheet]
    (:data sheet)))

; :state of the :active-sheet
(reg-sub
  :active-state
  :<- [:active-sheet]
  (fn [sheet [_ & ks]]
    (or (get-in sheet (cons :state ks))
        (when-not ks
          {}))))

; specific page in the :active-sheet
(reg-sub
  :active-page
  :<- [:active-data]
  (fn [data [_ page-id]]
    (let [page-id (or (:name page-id)
                      page-id)]
      (->> data
           :pages
           (filter (comp (partial = page-id) :name))
           first))))

; static data in the active sheet
(reg-sub
  :static
  (fn [[_ page-id _] _]
    (subscribe [:active-page page-id]))
  (fn [page [_ _ field]]
    (-> page :static field)))
