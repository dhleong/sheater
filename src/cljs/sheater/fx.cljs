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

(defn confirm-close-window
  [e]
  (let [confirm-message "You have unsaved changes. Are you sure you want to exit?"]
    (when e
      (set! (.-returnValue e) confirm-message))
    confirm-message))

(reg-fx
  :save-sheet!
  (fn [sheet]
    (when-let [provider-id (:provider sheet)]
      (-> providers
          provider-id
          :inst
          (save-sheet
            sheet
            (fn [err]
              (println "save-sheet result")
              (when err
                ; TODO notify? retry?
                (js/console.warn err))
              (when-not err
                (js/window.removeEventListener
                  "beforeunload"
                  confirm-close-window)
                (println "Saved!"))))))))

(defonce save-sheet-timers (atom {}))
(def throttled-save-timeout 7500)

(reg-fx
  :save-sheet-throttled!
  (fn [sheet-id]
    (when sheet-id
      (if-let [timer (get @save-sheet-timers sheet-id)]
        ; existing timer; clear it
        (js/clearTimeout timer)
        ; no existing, so this is the first; confirm window closing
        (js/window.addEventListener
          "beforeunload"
          confirm-close-window))
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
