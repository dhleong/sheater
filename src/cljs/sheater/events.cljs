(ns ^{:author "Daniel Leong"
      :doc "events"}
  sheater.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-cofx inject-cofx
                                   path trim-v after debug
                                   ->interceptor get-coeffect get-effect 
                                   assoc-coeffect assoc-effect
                                   dispatch]]
            [sheater.db :as db]))

(reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(reg-event-db
 :set-active-panel
 [trim-v]
 (fn [db active-panel]
   (println "Navigate: " active-panel)
   (assoc db :active-panel active-panel)))

(reg-event-db
  :assoc-provider!
  [trim-v]
  (fn [db [provider-id k v]]
    (assoc-in db [:providers provider-id k] v)))

;;
;; Sheet file management
;;

(reg-event-db
  :add-sheets
  [(path :sheets) trim-v]
  (fn [old-sheets [added-sheets]]
    (->> added-sheets
         (map (fn [sheet]
                [(:id sheet) sheet]))
         (into {})
         (merge old-sheets))))

(reg-event-db
  :add-sheet
  [(path :sheets) trim-v]
  (fn [sheets [sheet]]
    (assoc sheets (:id sheet) sheet)))

(reg-event-db
  :edit-sheet-page
  [(path :sheets) trim-v]
  (fn [sheets [sheet-id page-id new-page]]
    (update-in sheets
               [sheet-id :data :pages]
               (fn [pages-list]
                 ; TODO more efficient please?
                 (vec
                   (map
                     (fn [page]
                       (if (= page-id (:name page))
                         new-page
                         page))
                     pages-list))))))

(reg-event-db
  :edit-sheet-state
  [trim-v]
  (fn [db [k v]]
    (let [sheet-id (-> db
                       :active-panel
                       second)]
      (println "Edit" sheet-id k v)
      (update-in db
                 [:sheets sheet-id :data :state]
                 assoc
                 k v))))

(reg-event-db
  :delete-sheet!
  [(path :sheets) trim-v]
  (fn [sheets [sheet-id]]
    (dissoc sheets sheet-id)))

(reg-event-fx
  :refresh!
  [(path :sheets) trim-v]
  (fn [{:keys [db]} [sheet-id]]
    (let [sheets db]  ; path'd
      (when-let [sheet (get sheets sheet-id)]
        {:refresh! sheet}))))

(reg-event-fx
  :save-sheet!
  [(path :sheets) trim-v]
  (fn [{:keys [db]} [sheet-id]]
    (let [sheets db]  ; path'd
      (when-let [sheet (get sheets sheet-id)]
        {:save-sheet! sheet}))))
