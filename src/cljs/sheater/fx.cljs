(ns ^{:author "Daniel Leong"
      :doc "Effects"}
  sheater.fx
  (:require [re-frame.core :refer [reg-fx dispatch]]
            [sheater.provider :refer [providers]]
            [sheater.provider.proto :refer [refresh-sheet save-sheet]]))

;; -- Navigation --------------------------------------------------------------
;; For when you can't just use href
;;

(reg-fx
  :navigate!
  (fn [url]
    (println "navigate:" url)
    (set! js/window.location url)))

(reg-fx
  :navigate-replace!
  (fn [url]
    (js/window.location.replace url)))


;; -- Sheet Refresh -----------------------------------------------------------
;; For when we have the metadata of a sheet, but not its content
;;

(reg-fx
  :refresh!
  (fn [sheet]
    (when-let [provider-id (:provider sheet)]
      (-> providers
          provider-id
          :inst
          (refresh-sheet
            sheet
            (fn [contents]
              (dispatch [:add-sheet (assoc sheet
                                           :data contents)])))))))

;; -- Sheet Save --------------------------------------------------------------
;; Persist changes to a sheet
;;

(reg-fx
  :save-sheet!
  (fn [sheet]
    (when-let [provider-id (:provider sheet)]
      (-> providers
          provider-id
          :inst
          (save-sheet sheet)))))

(defonce save-sheet-timers (atom {}))
(def throttled-save-timeout 7500)

(reg-fx
  :save-sheet-throttled!
  (fn [sheet-id]
    (when sheet-id
      (when-let [timer (get @save-sheet-timers sheet-id)]
        (js/clearTimeout timer))
      (js/console.log "Queue throttled-save of" (str sheet-id))
      (swap! save-sheet-timers
             assoc
             sheet-id
             (js/setTimeout
               (fn []
                 (js/console.log "RUN throttled-save of" (str sheet-id))
                 (swap! save-sheet-timers dissoc sheet-id)
                 (dispatch [:save-sheet! sheet-id]))
               throttled-save-timeout)))))
