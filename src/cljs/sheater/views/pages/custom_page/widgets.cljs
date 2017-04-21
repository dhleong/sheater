(ns ^{:author "Daniel Leong"
      :doc "custom-page widgets"}
  sheater.views.pages.custom-page.widgets
  (:require [clojure.string :as str]
            [re-com.core :as rc]
            [re-frame.core :refer [subscribe dispatch]]))

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
  (if (:id item)
    item
    (assoc item :id (->id (:label item)))))

(defn selectable-list
  [opts]
  {:pre [(:items opts)
         (:id opts)]}
  (let [id (:id opts)
        items (->> opts
                   :items
                   (map ensure-id))
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
