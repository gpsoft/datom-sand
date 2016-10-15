(ns datom-sand.util
  (:import datomic.Util)
  (:require [datomic.api :as d]
            [clojure.java.io :as io]))


(comment
  (-> (io/resource "datom-sand/schema.edn")
      (io/reader)
      (Util/readAll))
  (read-edn "datom-sand/schema.edn")
  )

(defn read-edn
  [path]
  (-> (io/resource path)
      io/reader
      Util/readAll))

(defn tx-all
  [conn txds]
  (loop [[tx-data & more] txds]
    (when tx-data
      @(d/transact conn tx-data)
      (recur more))))
