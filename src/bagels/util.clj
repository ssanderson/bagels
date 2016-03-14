(ns bagels.util
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn home [] (System/getProperty "user.home"))

(defn expanduser
  [path]
  (if (.startsWith path "~")
    (str/replace-first path "~" (home))
    path))

(defn realpath
  [path]
  (-> (io/file (expanduser path))
      (.getCanonicalPath)))
