(ns sheater.templ.fun-test
  (:require [cljs.test :refer-macros [deftest testing is run-tests]]
            [sheater.templ.fun :refer [->fun]]))

(deftest ->fun-test
  (testing "Get exposed fun"
    (let [fun (->fun (symbol "keyword"))]
      (is (not (nil? fun)))
      (is (= "exported-keyword" (name fun))))))

