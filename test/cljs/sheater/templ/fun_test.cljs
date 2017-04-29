(ns sheater.templ.fun-test
  (:require [cljs.test :refer-macros [deftest testing is run-tests]]
            [cljs.nodejs :as node]
            [sheater.templ.fun :refer [->fun]]))

(deftest ->fun-test
  (testing "Run exposed fun"
    (is (= :im-a-keyword
           ((->fun (symbol "keyword"))
            "im-a-keyword")))))

