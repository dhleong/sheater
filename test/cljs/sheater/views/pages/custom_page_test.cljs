(ns sheater.views.pages.custom-page-test
  (:require [cljs.test :refer-macros [deftest testing is run-tests]]
            [cljs.nodejs :as node]
            [sheater.views.pages.custom-page :refer [render]]))

(deftest render-test
  (testing "FIXME new test"
    (let [spec [:rows
                [:cols
                 [:table
                  [:tr
                   [:td "Name"]
                   [:td [:input {:id :name
                                 :type :text}]]]]]
                 ]]
      (is (= 0 (render spec nil))))))

