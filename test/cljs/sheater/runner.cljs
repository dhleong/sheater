(ns sheater.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [sheater.templ.fun-test]
              [sheater.views.pages.custom-page-test]))

(doo-tests 'sheater.templ.fun-test
           'sheater.views.pages.custom-page-test)
