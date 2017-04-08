(ns ^{:author "Daniel Leong"
      :doc "events"}
  sheater.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-cofx inject-cofx
                                   path trim-v after debug
                                   ->interceptor get-coeffect get-effect 
                                   assoc-coeffect assoc-effect
                                   dispatch]]
            [sheater.db :as db]
            [sheater.subs :refer [sheet-by-id]]))

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
  :add-files ;; TODO rename to -sheets
  [trim-v]
  (fn [db [files]]
    (update db :sheets concat files)))

(declare remove-by-id)
(reg-event-db
  :add-sheet
  [trim-v]
  (fn [db [sheet]]
    (update db
            :sheets
            (fn [sheets-list]
              (conj
                (remove-by-id sheets-list (:id sheet))
                sheet)))))

(defn remove-by-id
  [sheets-list id]
  (remove
    (comp (partial = id) :id)
    sheets-list))
(reg-event-db
  :delete-sheet!
  [trim-v]
  (fn [db [sheet-id]]
    (update db :sheets remove-by-id sheet-id)))

(reg-event-fx
  :refresh!
  [trim-v]
  (fn [{:keys [db]} [sheet-id]]
    (let [sheet (sheet-by-id db sheet-id)]
      (when sheet
        {:refresh! sheet}))))
