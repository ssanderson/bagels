(ns bagels.db
  (:require [clojure.java.jdbc :as sql]
            [clojure.java.io :as io]
            [environ.core :refer [env]]))

(def home (System/getProperty "user.home"))

(defn expanduser
  [path]
  (if (.startsWith path "~")
    (.replaceFirst path "~" home)
    path))

(defn realpath
  [path]
  (-> (io/file (expanduser path))
      (.getCanonicalPath)))

(def settings
  {:classname "org.h2.Driver"
   :subprotocol "h2:file"
   :subname (str "file://" (realpath (:bagel-db-path env)))})

(defn init-schema
  "Initialize the bagel db schema."
  []
  (sql/with-db-connection [conn settings]
    (sql/db-do-commands
     conn true
     (sql/create-table-ddl
      :users
      [:id "bigint primary key auto_increment"]
      [:name "varchar"]))))

(init-schema)
