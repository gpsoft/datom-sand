(ns datom-sand.util
  (:import datomic.Util)
  (:require [datomic.api :as d]
            [clojure.java.io :as io]))

(defn empty-conn
  "実験用に、空っぽDBへのconnを得る。"
  []
  (let [db-name (gensym)
        uri (str "datomic:mem://" db-name)]
    (d/delete-database uri)
    (d/create-database uri)
    (d/connect uri)))

(defn read-edn
  "ednファイルを読み、S式のベクタを返す。"
  [path]
  (-> (io/resource path)
      io/reader
      Util/readAll))

(defn tx-all
  "txdsに含まれるすべてのtx-dataについてTxを実行。"
  [conn txds]
  (loop [[tx-data & more] txds]
    (when tx-data
      @(d/transact conn tx-data)
      (recur more))))

(defn mk-entity
  "実験用の、適当なエンティティを作り、IDを返す。"
  [conn doc]
  (let [tmp (d/tempid :db.part/user)
        {:keys [db-after tempids]}
        @(d/transact conn
                     [[:db/add tmp
                       :db/doc doc]])]
    (d/resolve-tempid db-after tempids tmp)))

(defn entity
  "Entityオブジェクト、または特定の属性値を返す。"
  ([db eid]
   (->> eid
        (d/entity db)
        d/touch))
  ([db eid attr]
   (attr (entity db eid))))

(comment
  (-> (io/resource "datom-sand/schema.edn")
      (io/reader)
      (Util/readAll))
  (read-edn "datom-sand/schema.edn")
  )
