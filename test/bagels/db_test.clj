(ns bagels.db-test
  (:require[clojure.test :refer [deftest testing is]]
           [bagels.db :as db]))

(deftest test-defquery
  (testing "collect no parameters"
    (is (= (macroexpand-1
            '(db/defquery my-query
               "SELECT * from table;"))
           '(clojure.core/defn my-query [db-spec & {:keys []}]
              (clojure.java.jdbc/query
               db-spec
               ["SELECT * from table;"])))))
  (testing "collect single parameter"
    (is (= (macroexpand-1
            '(db/defquery my-query
               "SELECT * from table where column=" (? column)))
           '(clojure.core/defn my-query [db-spec & {:keys [column]}]
              (clojure.java.jdbc/query
               db-spec
               ["SELECT * from table where column=? " column]))))))
