(ns ^{:author "Daniel Leong"
      :doc "custom-page widgets"}
  sheater.views.pages.custom-page.widgets
  (:require [clojure.string :as str]
            [re-com.core :as rc]
            [re-frame.core :refer [subscribe dispatch]]))

;;
;; Utils
;;

(defn ->id
  [string]
  (-> string
      str/lower-case
      (str/replace #"[ ]+" "-")
      str/trim
      (str/replace #"[^a-z0-9-]" "")
      keyword))

(defn ensure-id
  [item]
  (cond
    (:id item) item
    (string? item) (ensure-id {:id item :label item})
    :else (assoc item :id (->id (:label item)))))

(defn ->items-with-ids
  "Given an opts map which is assumed to have an :items
   list, return a seq of items that each definitely have
   an :id"
  [opts]
  (->> opts
       :items
       (map ensure-id)))

(defn write-state
  [k v]
  (println k " <- " v)
  (dispatch [:edit-sheet-state k v]))


;;
;; Widgets
;;

(defn input
  "Basic text input widget"
  [opts]
  {:pre [(:id opts)]}
  (let [id (:id opts)]
    [rc/input-text
     :model (or @(subscribe [:active-state id])
                "")
     :on-change (partial write-state id)]))

(defn picker
  [opts]
  {:pre [(contains? opts :items)
         (:id opts)]}
  (let [id (:id opts)
        items (->items-with-ids opts)
        selected @(subscribe [:active-state id])]
    [rc/single-dropdown
     :choices items
     :filter-box? true
     :model selected
     :width "100%"
     :on-change (fn [new-selection]
                  (dispatch [:edit-sheet-state id new-selection]))]))

(defn selectable-list
  [opts]
  {:pre [(:items opts)
         (:id opts)]}
  (let [id (:id opts)
        items (->items-with-ids opts)
        selected-set @(subscribe [:active-state id])
        selected (->> items
                      (filter (fn [item]
                                (contains? selected-set
                                           (:id item))))
                      (sort-by :label))]
    [rc/v-box
     :children
     [(for [s selected]
        ^{:key (:id s)} [rc/h-box
                         :children
                         [(:label s)
                          (when-let [desc (:desc s)]
                            [rc/info-button
                             :info desc])
                          [rc/md-icon-button
                           :md-icon-name "zmdi-delete"
                           :size :smaller
                           :on-click
                           (fn []
                             (let [new-val (disj selected-set (:id s))]
                               (dispatch [:edit-sheet-state id new-val])))]]])
      [rc/single-dropdown
       :choices items
       :filter-box? true
       :model nil
       :placeholder (:placeholder opts "Select one")
       :on-change (fn [selected-id]
                    (let [new-val (if (set? selected-set)
                                    (conj selected-set selected-id)
                                    #{selected-id})]
                      (dispatch [:edit-sheet-state id new-val])))]]]))
