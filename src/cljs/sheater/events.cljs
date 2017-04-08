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
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(reg-event-db
  :assoc-provider!
  [trim-v]
  (fn [db [provider-id k v]]
    (assoc-in db [:providers provider-id k] v)))
