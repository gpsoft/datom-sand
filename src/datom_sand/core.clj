(ns datom-sand.core
  (:require [datomic.api :as d]
            [datom-sand.util :as u]))

(defn all-customers
  [db]
  (d/q '[:find (pull ?c [*])
         :where
         [?c :customer/name]]
       db))

(defn all-invitations
  [db]
  (d/q '[:find ?invitor ?invitee
         :where
         [?c :customer/name ?invitee]
         [?c :customer/invited-by ?cc]
         [?cc :customer/name ?invitor]]
       db))

(defn all-items
  [db]
  (d/q '[:find [?n ...]
         :where
         [_ :item/name ?n]]
       db))

(defn find-idents-in-set
  "すべての別名を見つける。"
  [db]
  (d/q '[:find ?v ?t
         :in $
         :where
         [$ _ :db/ident ?v ?t]]
       db))

(defn find-attributes-in-vec
  "すべての属性名を見つける。"
  [db]
  (d/q '[:find [?an ...]
         :in $
         :where
         [$ :db.part/db :db.install/attribute ?a]
         [$ ?a :db/ident ?an]]
       db))

(defn find-one-partitions
  "パーティション名を1つ見つける。"
  [db]
  (d/q '[:find ?pn .
         :in $
         :where
         [$ :db.part/db :db.install/partition ?p]
         [$ ?p :db/ident ?pn]]
       db))
