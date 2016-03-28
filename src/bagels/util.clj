(ns bagels.util
  (:require [clojure.java.io :as io]
            [clojure.test :refer [is]]
            [clojure.set :refer [difference]]
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

(defn contains-all?
  [m keys]
  (every? #(contains? m %) keys))

(defn format-map
  "Format a string from a map and a sequence of keys."
  [m s ks]
  (if-let [missing-ks (seq (difference (set ks) (set (keys m))))]
    (throw (IllegalArgumentException.
            (format "Expected map to contain keys %s, but %s were missing"
                    ks missing-ks)))
    (apply format s ((apply juxt ks) m))))

