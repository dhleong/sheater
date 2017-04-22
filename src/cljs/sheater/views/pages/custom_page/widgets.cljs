(ns ^{:author "Daniel Leong"
      :doc "custom-page widgets"}
  sheater.views.pages.custom-page.widgets
  (:require [clojure.string :as str]
            [reagent.core :as reagent]
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

(def input-class-spec
  {"number" {:regex #"[0-9]+"
             :width "3em"}
   "big-number" {:regex #"[0-9]+"
                 :width "6em"}})

(defn input
  "Basic text input widget"
  [opts]
  {:pre [(:id opts)]}
  (let [id (:id opts)
        class (:class opts)
        regex (get-in input-class-spec [class :regex])]
    (when regex
      (println id class regex))
    [rc/input-text
     :class class
     :width (get-in input-class-spec [class :width])
     :model (or @(subscribe [:active-state id])
                "")
     :on-change (partial write-state id)
     :validation-regex regex]))

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

(defn selectable-set-base
  [opts wrap-children]
  {:pre [(contains? opts :items)
         (:id opts)]}
  (let [show-picker? (reagent/atom false)
        mouse-over? (reagent/atom false)]
    (fn []
      (let [id (:id opts)
            items (->items-with-ids opts)
            selected-set @(subscribe [:active-state id])
            selected (->> items
                          (filter (fn [item]
                                    (contains? selected-set
                                               (:id item))))
                          (sort-by :label))
            is-mouse-over? @mouse-over?]
        (wrap-children
          mouse-over?
          [(for [s selected]
             ^{:key (:id s)} [rc/h-box
                              :children
                              [(:label s)
                               (when-let [desc (:desc s)]
                                 [rc/info-button
                                  :info desc])
                               [rc/row-button
                                :md-icon-name "zmdi-delete"
                                :mouse-over-row? is-mouse-over?
                                :on-click
                                (fn []
                                  (let [new-val (disj selected-set (:id s))]
                                    (dispatch [:edit-sheet-state id new-val])))]]])
           (with-meta
             (if @show-picker?
               [rc/single-dropdown
                :choices items
                :filter-box? true
                :model nil
                :placeholder (:placeholder opts "Select one")
                :on-change (fn [selected-id]
                             (let [new-val (if (set? selected-set)
                                             (conj selected-set selected-id)
                                             #{selected-id})]
                               (reset! show-picker? false)
                               (dispatch [:edit-sheet-state id new-val])))]
               [rc/md-icon-button
                :md-icon-name "zmdi-plus"
                :size :smaller
                :on-click #(reset! show-picker? true)])
             {:key :-add-new-element})])))))

(defn selectable-set
  [opts]
  (selectable-set-base
    opts
    (fn [mouse-over? kids]
      [:div.selectable-set
       {:on-mouse-over #(reset! mouse-over? true)
        :on-mouse-out #(reset! mouse-over? false)}
       (seq kids)])))

(defn selectable-list
  [opts]
  (selectable-set-base
    opts
    (fn [mouse-over? kids]
      [rc/v-box
       :attr
       {:on-mouse-over #(reset! mouse-over? true)
        :on-mouse-out #(reset! mouse-over? false)}
       :children kids])))
