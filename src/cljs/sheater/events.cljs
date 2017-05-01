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

(reg-event-fx
  :navigate!
  [trim-v]
  (fn [_ [url]]
    {:navigate! url}))

(reg-event-fx
  :navigate-replace!
  [trim-v]
  (fn [_ [url]]
    {:navigate-replace! url}))

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
  :edit-sheet
  [(path :sheets) trim-v]
  (fn [sheets [sheet-id new-sheet]]
    (assoc-in sheets
              [sheet-id :data]
              new-sheet)))

(reg-event-db
  :edit-sheet-static
  [(path :sheets) trim-v]
  (fn [sheets [sheet-id new-static]]
    (assoc-in sheets
              [sheet-id :data :static]
              new-static)))

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

(reg-event-fx
  :edit-sheet-state
  [trim-v]
  (fn [{:keys [db]} [k v]]
    (let [active-panel (:active-panel db)
          viewer? (= :viewer (first active-panel))
          sheet-id (-> active-panel
                       second  ; [sheet, page] pair
                       first)] ; sheet
      ; don't bother saving if there was no change
      (when-not (= v (get-in db [:sheets sheet-id :data :state k]))
        (println "Edit" sheet-id k v "(viewer?" viewer? ")")
        {:db (update-in db
                        [:sheets sheet-id :data :state]
                        assoc
                        k v)
         :save-sheet-throttled! (when viewer?
                                  sheet-id)}))))

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

;;
;; Notes

(reg-event-db
  :delete-active-note!
  [trim-v]
  (fn [db [note]]
    (let [active-panel (:active-panel db)
          [active-sheet active-page] (second active-panel)
          sheets (-> db :sheets)
          note-created (:created note)]
      (update-in db [:sheets active-sheet
                     :data :state :sheater/notes
                     active-page]
                 (comp
                   vec
                   (partial
                     remove
                     (fn [candidate]
                       (= note-created (:created candidate)))))))))

(reg-event-db
  :update-active-note!
  [trim-v]
  (fn [db [note]]
    (let [active-panel (:active-panel db)
          [active-sheet active-page] (second active-panel)
          sheets (-> db :sheets)]
      (update-in db [:sheets active-sheet
                     :data :state :sheater/notes
                     active-page]
                 (fn [notes]
                   (let [created (:created note)
                         [idx] (->> notes
                                      (map-indexed list)
                                      (filter
                                        (fn [[i n]]
                                          (= created (:created n))))
                                      first)]
                     (cond
                       (not notes) [note]
                       (not (nil? idx)) (assoc (vec notes) idx note)
                       :else (concat notes [note]))))))))
