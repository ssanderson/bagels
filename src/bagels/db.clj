(ns bagels.db
  (:require [bagels.util :refer [realpath expanduser]]
            [clojure.core :refer [filter list? string? symbol?]]
            [clojure.string :refer [split]]
            [clojure.java.jdbc :as sql]
            [clojure.java.io :as io]
            [clojure.tools.macro :as macro]
            [environ.core :refer [env]]))

(def default-settings
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
        notnull (fn [& spec] (conj (vec spec) "NOT NULL"))
        fkey (fn [source-column]
               (let [source-column (name source-column)
                     [target-table target-column] (split source-column #"_")]
                 [(apply format "FOREIGN KEY (%s) REFERENCES %s(%s)"
                         (map name [source-column target-table target-column]))]))]
    {:user [(pkey :id)
            (notnull :name :varchar)
            (notnull :email :varchar)]
     :bagel [(pkey :id)
             (notnull :name :varchar)]
     :bagelpref [(pkey :id)
                 (notnull :user_id :int)
                 (notnull :bagel_id :int)
                 (fkey :user_id)
                 (fkey :bagel_id)]
     :creamcheese [(pkey :id)
                   (notnull :name :varchar)]
     :creamcheesepref [(pkey :id)
                       (notnull :value :int)
                       (notnull :user_id :int)
                       (notnull :creamcheese_id :int)
                       (fkey :user_id)
                       (fkey :creamcheese_id)]}))

(defn- schema-to-sql
  "Convert a map from table -> column -> description into a sequence of CREATE
  TABLE statements."
  [schema]
  (let [spec-to-sql (fn [[table-name column-specs]]
                      (apply sql/create-table-ddl table-name column-specs))]
    (map spec-to-sql schema)))

(defn create-tables
  "Create tables from the bagel schema."
  [settings]
  (sql/with-db-connection [conn settings]
    (let [statements (schema-to-sql bagel-schema)]
      (apply sql/db-do-commands conn true statements))))

(defn- param?
  [l]
  (and (= (first l) '?)
       (symbol? (second l))))

(defn- extract-param
  "Extract a parameter from defsql token.

  If the token is a string, yield nil, indicating no parameter associated with
  the token.

  If the token is a pair of symbols whose first entry is '?, yield the second
  entry.

  Otherwise, raise an error."
  [elem]
  (cond
    (string? elem) nil
    (param? elem) (second elem)
    :else (throw
           (ex-info
            (format
             "Bad in defsql. Expected string or '(? param) pair. Got %s." elem)
            {:cause elem}))))


(defn- collect-params
  [tokens]
  (map vector (filter identity (map extract-param tokens)) (range)))

(defn- defsql-arglist
  "Helper for parsing substitution parameters from a query string."
  [query]
  (let [params (filter list? query)]
    nil))

(defn- defsql-impl
  "Real implementation of defsql. Converts function name and query text into a
  function that takes a settings map plus arguments for all placeholders in the
  query text."
  [name query]
  (let [query (eval query)]
    `(defn ~name
       ~(defsql-arglist query))))

(defmacro defsql
  "Macro for creating simple sql queries."
  ([name & query]
   nil))
  ;;(defsql-impl name query)))

(defsql list-tables
  "SELECT * from information_schema.tables "
  "where table_schema=" (? schema_name))

;; (defn list-tables
;;   "List existing tables in the bagel DB."
;;   (sql/with-db-connection [conn settings]
;;     (sql/query conn "SELECT * from information_schema.tables
