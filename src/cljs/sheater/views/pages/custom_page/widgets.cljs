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

(defn ->state
  [id]
  (get @(subscribe [:active-state]) id))

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

(defn checkbox
  [opts]
  {:pre [(or (:id opts)
             (contains? opts :value))]}
  (if-let [id (:id opts)]
    [rc/checkbox
     :model (->state id)
     :on-change (partial write-state id)]
    (let [checked? (:value opts)]
      [:i
       {:class (str
                 "zmdi zmdi-check"
                 (when-not checked?
                   " invisible"))}])))

(defn cols
  "Render each child as a column"
  [children]
  (let [cols-count (count children)
        col-width (int (/ 12 cols-count))
        wrapper-element (keyword
                          (str "div.col-md-" col-width))]
    [:div.row
     (for [[i child] (map-indexed list children)]
       ^{:key i}
       [wrapper-element child])]))

(def input-class-spec
  {"number" {:regex #"^[0-9]*$"
             :width "4em"}
   "big-number" {:regex #"^[0-9]*$"
                 :width "7em"}})

(defn input
  "Basic text input widget"
  [opts]
  {:pre [(:id opts)]}
  (let [id (:id opts)
        class (:class opts)
        regex (get-in input-class-spec [class :regex])]
    [rc/input-text
     :class class
     :width (or (:width opts)
                (get-in input-class-spec [class :width]))
     :model (or (->state id) "")
     :on-change (partial write-state id)
     :validation-regex regex]))

(defn input-calc
  "input-calc is like a fancy input.number (or input.big-number)
   that when selected, pops up a calculator for easy editing."
  [opts]
  {:pre [(:id opts)]}
  (let [id (:id opts)
        class (or (:class opts)
                  "number")
        regex (get-in input-class-spec ["number" :regex])
        show-calc? (reagent/atom false)]
    (fn []
      [rc/popover-anchor-wrapper
       :showing? show-calc?
       :position :right-above

       :anchor
       [rc/input-text
        :class class
        :width (or (:width opts)
                   (get-in input-class-spec [class :width]))
        :model (or (str (->state id)) "")
        :on-change (partial write-state id)
        :attr {:on-click
               (fn [e]
                 (.preventDefault e)
                 (reset! show-calc? true))}
        :validation-regex regex]

       :popover
       [rc/popover-content-wrapper
        :title "Adjust Value"
        :on-cancel #(reset! show-calc? false)
        :no-clip? true

        :body
        [:div [rc/input-text
               :model ""
               :width "150px"
               :attr {:auto-focus true}
               :placeholder "+/- number"
               :on-change (fn [v]
                            (when-let [amount (js/parseInt v)]
                              (reset! show-calc? false)
                              (when-not (js/isNaN amount)
                                (println "CHANGE!" amount)
                                (write-state
                                  id (min
                                       (:max opts 99999999)
                                       (max
                                         (:min opts 0)
                                         (+ (js/parseInt (->state id))
                                            amount)))))))
               :validation-regex #"[-+]?[0-9]*"]]]])))


(defn picker
  [opts]
  {:pre [(contains? opts :items)
         (:id opts)]}
  (let [id (:id opts)
        items (->items-with-ids opts)
        selected (->state id)]
    [rc/single-dropdown
     :choices items
     :filter-box? true
     :model selected
     :width "100%"
     :on-change (fn [new-selection]
                  (dispatch [:edit-sheet-state id new-selection]))]))

(defn dynamic-table
  "A dynamic table is one whose values are added in a dialog, for
   which you may provide suggestions."
  [opts]
  {:pre [(:id opts)
         (:cols opts)]}
  ;; FIXME: This is waaaaay too complicated and should probably be
  ;; broken up into several smaller components....
  (let [id (:id opts)
        choices (:items opts)
        columns (:cols opts)
        show-prompt? (reagent/atom false)
        header-row (vec
                     (cons :tr
                           (concat
                             (map (fn [label]
                                    [:th (when-not (= :desc label)
                                           label)])
                                  columns)
                             [[:th]])))
        empty-new-row (vec
                          (map
                            (constantly "")
                            columns))
        new-row-value (reagent/atom empty-new-row)
        desc-col (.indexOf columns :desc)]
    (fn [opts]
      (let [items (->state id)
            auto-value (:value opts)
            auto-value-count (count auto-value)]
        [:table
         [:tbody

          ;; Render items:
          header-row

          (for [[i item] (map-indexed
                           list
                           (concat auto-value items))]
            (with-meta
              ; complicated fanciness to build:
              ; [:tr [:td ...] [:td [:delete-button]]]
              (vec
                (cons
                  :tr
                  (concat
                    (map-indexed
                      (fn [col-index part]
                        (if (= col-index desc-col)
                          ; special :desc col is wrapped in an info button
                          [:td
                           [rc/info-button
                            :info part]]
                          ; regular col
                          [:td part]))
                      item)
                    [[:td
                      (when (>= i auto-value-count)
                        [rc/row-button
                         :md-icon-name "zmdi-delete"
                         :mouse-over-row? true
                         :on-click
                         (fn [row]
                           (when-let [idx (if-let [i (.indexOf items item)]
                                            (when (not= -1 i) i))]
                             ; remove the first instance of the item from the
                             ; items vector
                             (write-state
                               id
                               (vec
                                 (concat
                                   (subvec items 0 idx)
                                   (subvec items (inc idx)))))))])]])))
              {:key (first item)}))

          ;; Allow new inputs:
          [:tr
           [:td
            {:colSpan (count columns)
             :style {:text-align :center}}
            [rc/popover-anchor-wrapper
             :showing? show-prompt?
             :anchor [rc/md-icon-button
                      :md-icon-name "zmdi-plus"
                      :on-click #(reset! show-prompt? true)]
             :position :right-below

             ;; The pick/input item popover prompt
             :popover
             [rc/popover-content-wrapper
              :title "Add New"
              :on-cancel #(reset! show-prompt? false)
              :body
              [:table
               [:tbody

                ;; picker:
                (when choices
                  [:tr
                   [:td {:colSpan (count columns)
                         :text-align :center}
                    [rc/single-dropdown
                     :choices (map
                                (fn [item]
                                  {:id (first item)
                                   :label (first item)})
                                choices)
                     :filter-box? true
                     :model nil
                     :placeholder "Pick a value"
                     :width "100%"
                     :on-change (fn [first-val]
                                  (let [vect (->> choices
                                                  (filter
                                                    (comp (partial = first-val)
                                                          first))
                                                  first)]
                                    (reset! show-prompt? false)
                                    (write-state id (conj items vect))))]]])
                (when choices
                  [:tr
                   [:td {:colSpan (count columns)
                         :text-align :center}
                    " - Or -"]])

                ;; manual input:
                header-row
                (vec
                  (cons
                    :tr
                    (map-indexed
                      (fn [index label]
                        (let [desc? (= :desc label)]
                          [:th
                           [(if desc?
                              rc/input-textarea
                              rc/input-text)
                            :model (nth @new-row-value index)
                            :placeholder (if (= :desc label)
                                           "Long Description"
                                           (str label))
                            :width (if desc?
                                     "170px"
                                     "150px")
                            :on-change
                            (fn [new-value]
                              (swap! new-row-value
                                     assoc
                                     index
                                     new-value))]]))
                      columns)))
                [:tr
                 [:td
                  {:colSpan (count columns)}
                  [rc/button
                   :label "Add!"
                   :on-click (fn []
                               (let [new-row @new-row-value]
                                 (reset! show-prompt? false)
                                 (reset! new-row-value empty-new-row)
                                 (write-state id (conj items new-row))))]]]]]]]]]]]))))

(defn selectable-set-base
  [opts wrap-children]
  {:pre [(contains? opts :items)
         (:id opts)]}
  (let [show-picker? (reagent/atom false)
        mouse-over? (reagent/atom false)]
    (fn [opts]
      (let [id (:id opts)
            items (->items-with-ids opts)
            selected-set (->state id)
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

(defn partial-number
  "A 'partial number' has a current value and a max value.
   The id of the max value is this (str id '-max')"
  [opts]
  {:pre [(:id opts)]}
  (let [id (:id opts)
        class (or (:class opts)
                  "number")
        max-id (keyword (str (name id) "-max"))]
    [rc/v-box
     :width "90px"
     :children
     [[input-calc {:id id
                   :width "100%"
                   :align :center
                   :class class
                   :min (:min opts 0)
                   :max (->state max-id)}]
      [rc/h-box
       :justify :center
       :children
       ["Max:"
        [input {:id max-id
                :class class}]]]]]))
