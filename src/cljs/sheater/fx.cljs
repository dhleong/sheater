(ns ^{:author "Daniel Leong"
      :doc "Effects"}
  sheater.fx
  (:require [re-frame.core :refer [reg-fx dispatch]]
            [sheater.provider :refer [providers]]
            [sheater.provider.proto :refer [refresh-sheet save-sheet]]))

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
