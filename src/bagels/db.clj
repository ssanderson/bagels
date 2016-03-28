(ns bagels.db
  (:require [bagels.util :refer [realpath expanduser]]
            [clojure.core :refer [filter list? string? symbol?]]
            [clojure.string :refer [split, join]]
            [clojure.java.jdbc :as sql]
            [clojure.java.io :as io]
            [clojure.tools.macro :as macro]
            [environ.core :refer [env]]))

(defn default-settings
  []
  {:classname "org.h2.Driver"
   :subprotocol "h2:file"
   :subname (str "file://" (realpath (:bagel-db-path env)))})

(def bagel-schema
  "The Bagel DB Schema.

  We have entity tables for users, bagels, and cream-cheeses, and we have
  many-to-many join tables mapping users to their 1-10 ratings of each kind of
  bagel and each cream cheese flavor."
  ;; Helpers for primary key, notnull, and foreign-key constraints.
  (let [pkey (fn [name] [name :int :primary :key :auto_increment])
        fkey (fn [source-column target-table target-column]
               [(apply format "FOREIGN KEY (%s) REFERENCES %s(%s)"
                       (map name [source-column target-table target-column]))])]
    {:user [(pkey :id)
            [:email :varchar :not :null :unique]]
     :bagel [(pkey :id)
             [:flavor :varchar :not :null :unique]]
     :bagel_pref [(pkey :id)
                  [:user_id :int :not :null]
                  [:bagel_id :int :not :null]
                  (fkey :user_id :user :id)
                  (fkey :bagel_id :bagel :id)]
     :cream_cheese [(pkey :id)
                    [:flavor :varchar :not :null]]
     :cream_cheese_pref [(pkey :id)
                         [:value :int :not :null]
                         [:user_id :int :not :null]
                         [:cream_cheese_id :int :not :null]
                         (fkey :user_id :user :id)
                         (fkey :cream_cheese_id :cream_cheese :id)]}))

(defn- schema-to-sql
  "Convert a map from table -> column -> description into a sequence of CREATE
  TABLE statements."
  [schema]
  (let [spec-to-sql (fn [[table-name column-specs]]
                      (apply sql/create-table-ddl table-name column-specs))]
    (map spec-to-sql schema)))

(defn create-tables
  "Create tables for the bagel schema."
  [settings]
  (sql/with-db-connection [conn settings]
    (let [statements (schema-to-sql bagel-schema)]
      (apply sql/db-do-commands conn true statements))))

(defn- param?
  "Is this token a parameter token for defquery?"
  [l]
  (and (= (first l) '?)
       (symbol? (second l))))

(def param-symbol second)

(defn- bad-query-elem
  [elem]
  (ex-info
   (format
    "Bad token in defquery. Expected string or '(? param) pair. Got %s." elem)
   {:cause elem}))

(defn collect-params
  "Collect parameter forms from a query."
  [tokens]
  (into [] (->> tokens
                (map #(cond
                        (string? %) nil
                        (param? %) (param-symbol %)
                        :else (throw (bad-query-elem %))))
                (keep identity))))

(defn- defquery-signature
  "Generate the signature form for query."
  [spec-arg-name params]
  (let [keywords (vec (distinct params))]
    `[~spec-arg-name & {:keys ~keywords}]))

(defn- defquery-body
  "Generate the body form for defquery."
  [spec-arg-name query params]
  (let [sql-text (join "" (map #(if (param? %) "? " %) query))
        query-args (into [sql-text] params)]
    `(sql/query ~spec-arg-name ~query-args)))

(defn- defquery-impl
  "Real implementation of defquery. Converts a function name and query text
  into a function that takes a settings map plus arguments for all placeholders
  in the query text."
  [name query]
  (let [params (collect-params query)
        spec-arg-name 'db-spec]
    `(defn ~name
       ~(defquery-signature spec-arg-name params)
       ~(defquery-body spec-arg-name query params))))

(defmacro defquery
  "Macro for creating simple sql queries."
  ([name & forms]
   (defquery-impl name forms)))

(defquery get-user
  "SELECT * FROM user where email=" (? email))
