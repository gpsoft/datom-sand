(ns datom-sand.sand
  (:require [datomic.api :as d]
            [datom-sand.core :as c]
            [datom-sand.util :as u]))

(defonce uri-in-memory "datomic:mem://hello")
(defonce uri-local-disk "datomic:free://localhost:4334/hello")

(defonce uri uri-in-memory)

(d/delete-database uri)
(d/create-database uri)

(def conn (d/connect uri))
(def db (d/db conn))

(comment
  (println (u/read-edn "datom-sand/schema.edn"))
  (->> (u/read-edn "datom-sand/schema.edn")
       (u/tx-all datom-sand.sand/conn))
  (->> (u/read-edn "datom-sand/customers.edn")
       (u/tx-all datom-sand.sand/conn))
  (->> (u/read-edn "datom-sand/items.edn")
       (u/tx-all datom-sand.sand/conn))
  )

(comment
  (c/all-customers (d/db conn))
  (c/all-invitations (d/db conn))
  (c/all-items (d/db conn))
  (c/add-customer conn "Hoge" :customer.pref/Fukuoka)
  (c/add-customer conn "Fuga" :customer.pref/Hiroshima "Hoge")
  )

(comment
  (c/find-idents-in-set db)
  (count (c/find-idents-in-set db))
  (c/find-attributes-in-vec db)
  (count (c/find-attributes-in-vec db))
  (c/find-one-partitions db)
  )

(comment
  (d/q '[:find ?a ?v ?tx ?added?
         :where [11 ?a ?v ?tx ?added?]]
       db)
  (d/entid db :db/ident)
  (d/touch (d/entity db :db.part/db))
  (d/q '[:find ?e
         :where [?e :db/unique :db.unique/identity]]
       db)
  (d/q '[:find (count ?n)
         :where
         [:db.part/db :db.install/attribute ?a]
         [?a :db/ident ?n]]
       db)
  (d/q '[:find ?e
         :where [?e :db/txInstant]]
       db)
  (d/q '[:find ?e
         :where [?e :db/ident :db/txInstant]]
       db))

(defn find-txs []
  (let [ts (d/q '[:find [?tx ?t]
                  :where [?tx :db/txInstant ?t]]
                db)]
    ts
    #_(map d/tx->t ts)))

(def datom [:db/add (d/tempid :db.part/user) :db/doc "hello world"])

(comment
  (def all-idents
    '[:find ?e
      :in $
      :where
      [$ ?e :db/ident]])
  (def all-idents'
    '[:find ?e
      :where
      [?e :db/ident]])
  (d/q all-idents' db))


(comment
  (d/attribute db 50)
  (d/ident db 50)
  (->> (d/entity db 50)
       (d/touch))

  (d/q '[:find ?a .
         :in $
         :where
         [:db.part/db :db.install/attribute ?n]
         [(datomic.api/attribute $ ?n) ?a]]
       db)
  (d/q '[:find [?n ...]
         :where
         [_ :db/ident ?n]
         [(str ?n) ?s]
         [(re-matches #"^:db.type.+$" ?s)]]
       db)
  (d/q '[:find [?n ...]
         :where
         [50 :db/ident ?n]]
       db)
  (d/q '[:find [?a ...]
         :in $
         :where
         [?e :db/ident :db.part/db]
         [(datomic.api/entity $ ?e) ?a]]
       db)
  (d/q '[:find [?v ...]
         :where
         [?e :db/doc ?v]]
       db)
  @(d/transact conn [datom])
  (d/q '[:find ?d
         :where
         [_ :db/doc ?d]
         [(re-matches #"^hello.+$" ?d)]]
       db))

