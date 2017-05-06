(ns ^{:author "Daniel Leong"
      :doc "create"}
  sheater.views.create
  (:require [cljs.reader :as edn]
            [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as rc]
            [sheater.provider :refer [providers]]
            [sheater.provider.proto :refer [create-sheet]]
            [sheater.views.header :refer [header-bar]]
            [ajax.core :refer [GET]]))

(def templates
  (->> [;;
        ;; add pairs of 'Template Name' -> 'templatefile.edn' here:
        ;;

        ["FantasyAGE" "fantasy-age.edn"]

        ;;
        ;; /end templates list
        ]

       ; wrap into a nice object, and sort by label
       (map
         (fn [[label file]]
           {:id label
            :label label
            :file file}))
       (sort-by :label)

       ; append the "Custom" template
       ;; TODO support custom templates
       #_(#(concat %
                 [{:id :custom
                   :label "Custom Template"}]))))

;;
;; Creation callbacks
;;

(defn do-create
  [template-data config]
  (println "Submit" config "with" (count (:pages template-data)) "pages")
  (let [provider-id (:provider config)]
    (-> providers
        provider-id
        :inst
        (create-sheet
          (assoc config
                 :template template-data)
          (fn on-complete [sheet]
            (println "Got sheet!" sheet)
            (dispatch [:add-sheet sheet])
            (js/window.location.replace
              (str "#/sheets/"
                   (name (:id sheet))))
            (dispatch [:set-active-panel
                       :viewer (name (:id sheet))]))))))

(defn load-and-create
  [creating? template-id config]
  (if-let [template-file (->> templates
                              (filter (comp (partial = template-id)
                                            :id))
                              first
                              :file)]
    (do
      (println "LOAD" template-id "@" template-file)
      (reset! creating? :loading-template)
      (GET (str "templates/" template-file)
           {:response-format :text
            :error-handler
            (fn [e]
              (js/console.error "Failed to load " template-file e)
              (reset! creating? {:error :template-load}))
            :handler
            (fn [raw-template]
              (try
                (let [template (edn/read-string raw-template)]
                  (reset! creating? :uploading)
                  (do-create template config))
                (catch :default e
                  (js/console.error "Failed to parse" raw-template)
                  (js/console.error e)
                  (reset! creating? {:error :template-parse}))))}))
    ; this shouldn't happen normally, but... let's log it, at least
    (js/console.error "No such template with id" template-id)))

;;
;; Form validation
;;

(defn- check
  [errors-atom field condition error-message]
  (when condition
    (reset! errors-atom {field error-message})
    (throw (js/Error error-message))))

(defn validate
  [errors-atom config]
  (check errors-atom :name
         (empty? (:name config))
         "Name is required")
  (check errors-atom :name
         (= "$static" (:name config))
         "\"$static\" is not a valid sheet name")

  ; TODO make sure there's no sheet with this name already

  ; all checks passed; erase errors and return the config unmolested
  (reset! errors-atom {})
  (println "All validate checks passed!")
  config)

;;
;; Main render function
;;

(defn panel
  []
  (let [default-provider (-> providers keys first)
        sheet-state (reagent/atom {:provider default-provider})
        creating? (reagent/atom false)
        selected-template (reagent/atom (:id (first templates)))
        template-data (reagent/atom nil)
        errors (reagent/atom {})
        ;
        set-key! (fn [k v]
                   (swap! sheet-state assoc k v))
        submit! (fn []
                  (let [template @template-data
                        config @sheet-state]
                    (try
                      (validate errors config)
                      (reset! creating? true)
                      (if (and (= :custom @selected-template)
                               template)
                        (do-create template config)
                        (load-and-create creating?
                                         @selected-template config))
                      (catch :default e
                        (println "Validation failed:" e)))))]
    (fn []
      (let [provider-id (:provider @sheet-state)
            provider-state @(subscribe [:provider provider-id])
            provider-info (-> providers provider-id)
            provider-loading? (nil? provider-state)
            provider-ready? (:ready? provider-state)]
        [rc/v-box
         :height "100%"
         :children
         [[header-bar
           {:header "New Sheet"}]
          [:div.container.card
           [:form
            {:on-submit (fn [e] (.preventDefault e))}
            [rc/h-box
             :children
             [[rc/v-box
               :gap ".5em"
               :children
               [[rc/label :label "Storage Provider"]
                [rc/single-dropdown
                 :choices (vals providers)
                 :model default-provider
                 :label-fn :name
                 :placeholder "Storage Provider"
                 :width "200px"
                 :on-change (partial set-key! :provider)]
                ;
                (cond
                  provider-loading? [rc/alert-box
                                     :id :no-provider
                                     :alert-type :warning
                                     :body "Provider loading..."]
                  (not provider-ready?)
                  [rc/alert-box
                   :id :provider-not-ready
                   :alert-type :danger
                   :body
                   [:div
                    [:div (:name provider-info) " is not configured."]
                    [rc/button
                     :class "btn-raised btn-secondary"
                     :label (str "Configure " (:name provider-info))
                     :on-click #(dispatch [:navigate!
                                           (str "#/provider/"
                                                (name provider-id))])]]]
                  :else nil)
                ;
                [rc/gap :size "1em"]
                [rc/label :label "Sheet Name"]
                [rc/input-text
                 :placeholder "Sheet Name"
                 :model ""
                 :on-change (partial set-key! :name)
                 :change-on-blur? false
                 :status-icon? true
                 :status (when (:name @errors) :error)
                 :status-tooltip (when-let [e (:name @errors)]
                                   e)
                 :validation-regex #"([a-zA-Z0-9_-]+)(.*)"]
                ;
                [rc/gap :size "1em"]
                [rc/label :label "Sheet Template"]
                [rc/single-dropdown
                 :choices templates
                 :model selected-template
                 :placeholder "Template"
                 :width "200px"
                 :on-change (partial reset! selected-template)]
                ;
                [rc/gap :size "1em"]
                [rc/button
                 :class "btn-raised btn-primary"
                 :label "Create"
                 :disabled? (not provider-ready?)
                 :on-click submit!]]]
              (when (= :custom @selected-template)
                [rc/v-box
                 :children
                 [[rc/label :label "Custom Template"]
                  [rc/input-textarea
                   :width "350px"
                   :rows 20
                   :model (str @template-data)
                   :on-change println]]])]]]]
          (when @creating?
            [rc/modal-panel
             :child
             [rc/v-box
              :width "300px"
              :align :center
              :children
              [[rc/title
                :label "Preparing your sheet..."
                :level :level2]
               [rc/gap :size "1em"]
               [rc/throbber :size :large]]]])]]))))
