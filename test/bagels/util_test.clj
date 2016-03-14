(ns bagels.util-test
  (:import java.io.File)
  (:require [clojure.core :refer [with-redefs]]
            [clojure.string :refer [replace-first]]
            [clojure.test :refer [deftest testing is]]
            [bagels.util :refer [expanduser realpath]]))

(deftest test-expanduser

  (testing "Do nothing if no leading ~"
    (doseq [input ["foo" "/foo/bar/buzz" "/foo/~/bar/buzz"]]
      (is (= input (expanduser input)))))

  (testing "Replace leading ~ with (home)"
    (doseq [input ["~/foo/" "~/foo/bar/buzz"]
            homedir ["/home/of/delicious/bagels" "/home/of/mediocre/bagels"]]
      (with-redefs [bagels.util/home (fn [] homedir)]
        (is (= (expanduser input) (replace-first input "~" homedir)))))))

;; TODO: test-realpath
