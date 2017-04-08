(ns sheater.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [sheater.core-test]))

(doo-tests 'sheater.core-test)
