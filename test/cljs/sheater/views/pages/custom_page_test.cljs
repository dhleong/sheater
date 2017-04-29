(ns sheater.views.pages.custom-page-test
  (:require [cljs.test :refer-macros [deftest testing is run-tests]]
            [cljs.nodejs :as node]
            [sheater.views.pages.custom-page :refer [render inflate-value-fn]]))

(deftest render-test
  #_(testing "FIXME new test"
    (let [spec [:rows
                [:cols
                 [:table
                  [:tr
                   [:td "Name"]
                   [:td [:input {:id :name
                                 :type :text}]]]]]
                 ]]
      (is (= 0 (render spec nil))))))

(deftest inflate-value-fn-test
  (testing "Inflate static refs"
    (let [page nil
          state nil
          opts {:symbols? true}
          form [:div :$melee-weapons]]
      (try
        (inflate-value-fn page state opts form)
        (catch :default e
          (println (.-stack e)))))
    #_(is (nil? (inflate-value-fn page state opts form)))))
