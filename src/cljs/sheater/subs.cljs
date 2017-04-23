(ns ^{:author "Daniel Leong"
      :doc "Subscriptions"}
  sheater.subs
  (:require [clojure.set :refer [union]]
            [re-frame.core :refer [reg-sub subscribe]]))

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

;; (reg-sub
;;   :sheet-page
;;   (fn [[_ sheet-id] _]
;;     (subscribe [:sheet sheet-id]))
;;   (fn [sheet, [_ _ page-id]]
;;     (->> sheet
;;          :data
;;          :pages
;;          (filter (comp (partial = page-id) :name))
;;          first)))

(reg-sub
  :active-sheet-id
  :<- [:active-panel]
  (fn [[panel arg] _]
    (when (or (= :viewer panel)
              (= :editor panel))
      (first arg))))

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
  :<- [:active-data]
  (fn [data [_ & ks]]
    (or (get-in data (cons :state ks))
        (when-not ks
          {}))))

; specific page in the :active-sheet
; NOTE: the page-id is optional; if not provided,
; it will use whatever page is provided in :active-panel
; Specifying page-id is deprecated in favor of this, though
; there are probably some places where that is not sufficient
(reg-sub
  :active-page
  :<- [:active-panel]
  :<- [:active-data]
  (fn [[panel data] [_ page-id]]
    (let [page-id (or (:name page-id)
                      page-id
                      (-> panel second second))]
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

;;
;; Notes

(reg-sub
  :active-notes
  :<- [:active-page]
  :<- [:active-state]
  (fn [[page state] _]
    (let [page-id (:name page)]
      (get-in state [:sheater/notes page-id]))))

(reg-sub
  :active-note-tags
  :<- [:active-notes]
  (fn [notes]
    (->> notes
         (map :tags)
         (apply union))))

(reg-sub
  :search-notes
  :<- [:active-notes]
  (fn [notes [_ query]]
    (println "SEARCH" _ query)
    ;; FIXME TODO
    notes))
