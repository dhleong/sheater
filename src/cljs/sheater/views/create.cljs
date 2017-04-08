(ns ^{:author "Daniel Leong"
      :doc "create"}
  sheater.views.create
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as rc]
            [sheater.provider :refer [providers]]
            [sheater.provider.proto :refer [create-sheet]]))

(defn title []
  [rc/title
   :label "New Sheet"
   :level :level1])

(defn panel
  []
  (let [default-provider (-> providers keys first)
        sheet-state (reagent/atom {:provider default-provider})
        creating? (reagent/atom false)
        set-key! (fn [k v]
                   (swap! sheet-state assoc k v))
        submit! (fn []
                  (reset! creating? true)
                  (println "Submit" @sheet-state)
                  (let [config @sheet-state
                        provider-id (:provider config)]
                    (-> providers
                        provider-id
                        :inst
                        (create-sheet
                          config
                          (fn on-complete [sheet]
                            (println "Got sheet!" sheet)
                            (dispatch [:add-sheet sheet])
                            (dispatch [:set-active-panel
                                       :viewer (:id sheet)]))))))]
    (fn []
      [rc/v-box
       :height "100%"
       :gap "1em"
       :children
       [[title]
        [:form
         {:on-submit (fn [e] (.preventDefault e))}
         [rc/v-box
          :gap ".5em"
          :children
          [[rc/label :label "Storage Provider"]
           ;
           [rc/single-dropdown
            :choices (vals providers)
            :model default-provider
            :label-fn :name
            :placeholder "Storage Provider"
            :width "200px"
            :on-change (partial set-key! :provider)]
           [rc/gap :size "1em"]
           ;
           [rc/label :label "Sheet Name"]
           [rc/input-text
            :placeholder "Sheet Name"
            :model ""
            :on-change (partial set-key! :name)
            :change-on-blur? false
            :validation-regex #"([a-zA-Z0-9_-]+)(.*)"]
           ;
           [rc/gap :size "1em"]
           [rc/button
            :label "Create"
            :on-click submit!]]]]
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
             [rc/throbber :size :large]]]])]])))
