(ns sheater.views.pages.custom-page-test
  (:require [cljs.test :refer-macros [deftest testing is run-tests]]
            [sheater.templ.fun :refer [->fun]]
            [sheater.views.pages.custom-page :refer [render inflate-value-fn process-source eval-form]]))

(deftest process-source-test
  (testing "Rename keyword-as-fn calls"
    (is (= "var res = new cljs.core.Keyword(null,\"req\",\"req\",(-326448303)).call(null,something)"
           (process-source
              "var res = new cljs.core.Keyword(null,\"req\",\"req\",(-326448303)).cljs$core$IFn$_invoke$arity$1(something)")))
    (is (= "other.call(foo, new cljs.core.Keyword(null,\"req\",\"req\",(-326448303)).call(null,a))"
           (process-source
              "other.call(foo, new cljs.core.Keyword(null,\"req\",\"req\",(-326448303)).cljs$core$IFn$_invoke$arity$1(a))")))
    (is (= (str "other.call(foo, "
                "new cljs.core.Keyword(null,\"req\",\"req\",(-326448303)).call(null,a),"
                "new cljs.core.Keyword(null,\"req\",\"req\",(-326448303)).call(null,b))")
           (process-source
             (str "other.call(foo, "
                  "new cljs.core.Keyword(null,\"req\",\"req\",(-326448303)).cljs$core$IFn$_invoke$arity$1(a),"
                  "new cljs.core.Keyword(null,\"req\",\"req\",(-326448303)).cljs$core$IFn$_invoke$arity$1(b))"))))))

(deftest eval-form-test
  (testing "Run exposed fun"
    (is (= :im-a-keyword
           (eval-form
             `(~(->fun (symbol "keyword"))
              "im-a-keyword"))))))

(deftest inflate-value-fn-test
  (testing "Inflate static refs"
    (let [page nil
          state nil
          opts {:symbols? true}
          form [:div :$melee-weapons]]
      (is (= [:div
              `(sheater.templ.fun/$->val :melee-weapons)]
             (inflate-value-fn page state opts form))))))
