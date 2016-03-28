(ns bagels.util-test
  (:import java.io.File)
  (:require [clojure.core :refer [with-redefs]]
            [clojure.string :refer [join replace-first]]
            [clojure.test :refer [deftest testing is]]
            [bagels.util :refer [contains-all? expanduser format-map realpath]]))

(deftest test-expanduser
  (testing "Do nothing if no leading ~"
    (doseq [input ["foo" "/foo/bar/buzz" "/foo/~/bar/buzz"]]
      (is (= input (expanduser input)))))
  (testing "Replace leading ~ with (home)"
    (doseq [input ["~/foo/" "~/foo/bar/buzz"]
            homedir ["/home/of/delicious/bagels" "n/home/of/mediocre/bagels"]]
      (with-redefs [bagels.util/home (fn [] homedir)]
        (is (= (expanduser input) (replace-first input "~" homedir)))))))

(deftest test-format-map
  (testing "Simple format-map"
    (doseq [a ["foo" "bar"]
            b ["buzz" "bazz"]]
      (let [m {:a a :b b :c "notused"}]
        (is (= (format-map m "%1$s %2$s %1$s" [:a :b])
               (join " " [a b a]))))))
  (testing "Duplicate keys"
    (doseq [a ["foo" "bar"]
            b ["buzz" "bazz"]]
      (let [m {:a a :b b :c "notused"}]
        (is (= (format-map m "%1$s %2$s %3$s" [:a :b :a])
               (join " " [a b a]))))))
  (testing "Missing keys"
    (let [m {:a "foo" :b "bar" :c "notused"}]
      (doseq [bad-keys [[:not-in-map] [:a :not-in-map]]]
        (is (thrown? IllegalArgumentException (format-map m "" bad-keys)))))))

(deftest test-contains-all?
  (let [m {:a 1 :b 2 :c nil :d false}
        subsets [[] [:a] [:c] [:c :d] [:a :b :c :d]]]
    (testing "Should succeeed"
      (doseq [good-keys subsets]
        (is (contains-all? m good-keys))))
    (testing "Should fail"
      (doseq [bad-keys (map #(conj % :not-in-map) subsets)]
        (is (not (contains-all? m bad-keys)))))))
