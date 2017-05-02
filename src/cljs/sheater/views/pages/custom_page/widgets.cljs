(ns ^{:author "Daniel Leong"
      :doc "custom-page widgets"}
  sheater.views.pages.custom-page.widgets
  (:require [clojure.string :as str]
            [reagent.core :as reagent]
            [re-com.core :as rc]
            [re-frame.core :refer [subscribe dispatch]]))

;;
;; Constants
;;

(def input-class-spec
  {"number" {:regex #"^[0-9]*$"
             :width "4em"}
   "big-number" {:regex #"^[0-9]*$"
                 :width "7em"}})

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
  (println "write-state:" k " <- " v)
  (dispatch [:edit-sheet-state k v]))


;;
;; Widgets
;;

(defn ^:export checkbox
  [opts]
  {:pre [(or (:id opts)
             (contains? opts :value))]}
  (if-let [id (:id opts)]
    [rc/checkbox
     :model (->state id)
     :on-change (partial write-state id)]
    (let [checked? (when-let [value-fn (:value opts)]
                     (value-fn))]
      [:i
       {:class (str
                 "zmdi zmdi-check"
                 (when-not checked?
                   " invisible"))}])))

(defn ^:export cols
  "Render each child as a column"
  [children]
  (let [cols-count (count children)
        col-width (int (/ 12 cols-count))
        wrapper-class (str "col-md-" col-width)]
    [:div.row
     (for [[i child] (map-indexed list children)]
       ^{:key i}
       [:div.cols-item
        {:class wrapper-class}
        [:div.hidden-md.hidden-lg.col-separator]
        child])]))

(declare dynamic-table)
(defn ^:export consumables
  "An inventory containing consumable items, such as potions
   or ammunition"
  [opts]
  {:pre [(:id opts)]}
  [dynamic-table
   {:id (:id opts)
    :cols [(:label opts)
           :amount]}])

(defn ^:export currency
  "Renders a nice table of currency values and
   does math for you."
  [opts]
  {:pre [(:id opts)
         (:kinds opts)]}
  ; TODO fancy math
  (let [id (:id opts)
        kinds (:kinds opts)
        state (->state id)
        number-width (get-in input-class-spec ["big-number" :width])
        number-regex (get-in input-class-spec ["big-number" :regex])]
    [:table
     [:tbody
      (vec
        (cons
          :tr
          (map
            (fn [kind]
              [:th (:label kind)])
            kinds)))
      (vec
        (cons
          :tr
          (map
            (fn [kind]
              [:td
               [rc/input-text
                :class "number"
                :style {:padding "0px"}
                :width number-width
                :model (str (or (get state (:id kind)) "0"))
                :on-change (fn [amount]
                             (write-state
                               id
                               (assoc state (:id kind) (js/parseInt amount))))
                :validation-regex number-regex]])
            kinds)))]]))

(defn ^:export input
  "Basic text input widget"
  [opts]
  {:pre [(:id opts)]}
  (let [id (:id opts)
        class (:class opts)
        regex (get-in input-class-spec [class :regex])]
    [rc/input-text
     :class class
     :style {:padding "0px"}
     :width (or (:width opts)
                (get-in input-class-spec [class :width]))
     :model (or (->state id) "")
     :on-change (partial write-state id)
     :validation-regex regex]))

(defn ^:export input-calc
  "input-calc is like a fancy input.number (or input.big-number)
   that when selected, pops up a calculator for easy editing."
  [opts]
  {:pre [(or (:id opts)
             (and (:model opts)
                  (:on-change opts)))]}
  (let [id (:id opts)
        class (or (:class opts)
                  "number")
        regex (get-in input-class-spec ["number" :regex])
        show-calc? (reagent/atom false)]
    (fn [opts]
      [rc/popover-anchor-wrapper
       :showing? show-calc?
       :position :right-above

       :anchor
       [rc/input-text
        :class class
        :width (or (:width opts)
                   (get-in input-class-spec [class :width]))
        :model (str
                 (or (:model opts)
                     (->state id)
                     ""))
        :on-change (or (:on-change opts)
                       (partial write-state id))
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
               :on-change
               (fn [v]
                 (when-let [amount (js/parseInt v)]
                   (reset! show-calc? false)
                   (when-not (js/isNaN amount)
                     ;; (println "CHANGE!" amount)
                     (if id
                       (write-state
                         id (min
                              (:max opts 99999999)
                              (max
                                (:min opts 0)
                                (+ (js/parseInt (->state id))
                                   amount))))
                       (do (println "FORWARD-CHANGE!")
                           ((:on-change opts) amount))))))
               :validation-regex #"[-+]?[0-9]*"]]]])))

(defn ^:export inventory
  "A container for items"
  [opts]
  {:pre [(:id opts)
         (:label opts)]}
  ; TODO support dragging between inventory containers
  [dynamic-table {:id (:id opts)
                  :cols [(:label opts)]}])

(defn ^:export picker
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

;; -- Dynamic Table ------------------------------------------------------------

(defn dynamic-table-prompt
  [id show-prompt? columns choices items header-row
   empty-new-row new-row-value]
  [rc/modal-panel
   :backdrop-on-click #(reset! show-prompt? nil)
   :wrap-nicely? false
   :child
   [rc/v-box
    :class "modal-content"
    :children
    [[:div.modal-header
      "Add New"]
     [:div.modal-body
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
                (let [desc? (= :desc label)
                      amount? (= :amount label)]
                  [:th
                   [(cond
                      desc? rc/input-textarea
                      :else rc/input-text)
                    :model (nth @new-row-value index)
                    :placeholder (cond
                                   desc?  "Long Description"
                                   amount? "Amount"
                                   :else (str label))
                    :validation-regex (when amount?
                                        (get-in input-class-spec
                                                ["number" :regex]))
                    :width (if desc?
                             "170px"
                             "150px")
                    :attr {:auto-focus (= index 0)
                           :tab-index "1"}
                    :on-change
                    (fn [new-value]
                      (swap! new-row-value
                             assoc
                             index
                             new-value))]]))
              columns)))]]
      [:div.modal-footer
       [rc/button
        :label "Cancel"
        :attr {:tab-index "3"}
        :on-click #(reset! show-prompt? false)]
       [rc/button
        :label "Add"
        :class "btn-primary"
        :attr {:tab-index "2"}
        :on-click
        (fn []
          (let [new-row @new-row-value]
            (reset! show-prompt? false)
            (reset! new-row-value empty-new-row)
            (write-state id (conj items new-row))))]]]]]])

(defn ^:export dynamic-table
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
                                    [:th (when-not
                                           (#{:desc :amount} label)
                                           label)])
                                  columns)
                             [[:th]])))
        empty-new-row (vec
                          (map
                            (constantly "")
                            columns))
        new-row-value (reagent/atom empty-new-row)
        mouse-over? (reagent/atom false)
        desc-col (.indexOf columns :desc)
        amount-col (.indexOf columns :amount)]
    (fn [opts]
      (let [items (->state id)
            auto-value (when-let [value-fn (:value opts)]
                         (value-fn))
            auto-value-count (count auto-value)
            is-mouse-over? @mouse-over?]
        [:table
         {:on-mouse-over #(reset! mouse-over? true)
          :on-mouse-out #(reset! mouse-over? false)}
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
                        (cond
                          ; special :desc col is wrapped in an info button
                          (= col-index desc-col)
                          [:td
                           [rc/info-button
                            :info part]]

                          ; special :amount col
                          (= col-index amount-col)
                          [:td
                           [input-calc
                            {:model part
                             :on-change
                             (fn [delta]
                               (write-state
                                 id
                                 (vec
                                   (update-in
                                     (vec items)
                                     [(- i auto-value-count) amount-col]
                                     (fn [old-value]
                                       (+ (js/parseInt old-value)
                                          (js/parseInt delta)))))))}]]

                          ; regular col
                          :else
                          [:td part]))
                      item)
                    [[:td
                      (when (>= i auto-value-count)
                        [rc/row-button
                         :md-icon-name "zmdi-delete"
                         :mouse-over-row? is-mouse-over?
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
            (when @show-prompt?
              [dynamic-table-prompt id
               show-prompt?
               columns choices items
               header-row empty-new-row
               new-row-value])
            [rc/md-icon-button
             :md-icon-name "zmdi-plus"
             :on-click #(reset! show-prompt? true)]]]]]))))

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

(defn ^:export selectable-set
  [opts]
  (selectable-set-base
    opts
    (fn [mouse-over? kids]
      [:div.selectable-set
       {:on-mouse-over #(reset! mouse-over? true)
        :on-mouse-out #(reset! mouse-over? false)}
       (seq kids)])))

(defn ^:export selectable-list
  [opts]
  (selectable-set-base
    opts
    (fn [mouse-over? kids]
      [rc/v-box
       :attr
       {:on-mouse-over #(reset! mouse-over? true)
        :on-mouse-out #(reset! mouse-over? false)}
       :children kids])))

(defn ^:export partial-number
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
