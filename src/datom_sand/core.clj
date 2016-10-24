(ns datom-sand.core
  (:require [datomic.api :as d]
            [datom-sand.util :as u]))

(defn all-customer-names
  "カスタマー名一覧"
  [db]
  (d/q '[:find [?n ...]
         :where
         [_ :customer/name ?n]]
       db))

(defn all-book-titles
  "書籍タイトル一覧"
  [db]
  (d/q '[:find [?t ...]
         :where
         [_ :book/title ?t]]
       db))

(defn all-reviews
  "ある書籍のレビュー一覧"
  [db book-title]
  (d/q '[:find ?review-title ?num-stars ?reviewer-name
         :in $ ?book-title
         :where
         [?b :book/title ?book-title]
         [?b :book/reviews ?r]
         [?r :review/title ?review-title]
         [?r :review/num-stars ?num-stars]
         [?r :review/reviewer ?reviewer]
         [?reviewer :customer/name ?reviewer-name]]
       db book-title))

(defn invite-customer
  "新しいカスタマーを紹介"
  [conn invitor-name {:keys [name pref]}]
  (d/transact
    conn
    [{:db/id #db/id [:db.part/user]
      :customer/name name
      :customer/pref pref
      :customer/invited-by [:customer/name invitor-name]}]))

(defn review-book
  "書籍をレビュー"
  [conn reviewer-name book-title {:keys [title num-stars]}]
  (d/transact
    conn
    [{:db/id #db/id [:db.part/user]
      :review/title title
      :review/num-stars num-stars
      :review/reviewer [:customer/name reviewer-name]
      :book/_reviews [:book/title book-title]}]))

(defn all-invitations
  [db]
  (->> (d/q '[:find ?invitor ?invitee
              :where
              [?c :customer/name ?invitee]
              [?c :customer/invited-by ?cc]
              [?cc :customer/name ?invitor]]
            db)
       (map (fn [[invitor invitee]] (str invitor " invited " invitee)))))

(defn all-items
  [db]
  (d/q '[:find [?n ...]
         :where
         [_ :item/name ?n]]
       db))

(defn add-customer
  ([conn n pref]
   (add-customer conn n pref nil))
  ([conn n pref invitor-name]
   (let [tx-data {:db/id #db/id [:db.part/db]
                  :customer/name n
                  :customer/pref pref}]
     @(d/transact conn
                  [(if invitor-name
                     (assoc tx-data :customer/invited-by [:customer/name invitor-name])
                     tx-data)]))))

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
