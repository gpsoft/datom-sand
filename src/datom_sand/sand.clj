(ns datom-sand.sand
  (:require [datomic.api :as d]
            [datom-sand.core :as core]
            [datom-sand.util :as util]))

(def conn (util/empty-conn))

;; スキーマ
(->> (util/read-edn "datom-sand/schema.edn")
     (util/tx-all conn))

;; サンプルデータ
(->> (util/read-edn "datom-sand/customers.edn")
     (util/tx-all conn))
(->> (util/read-edn "datom-sand/books.edn")
     (util/tx-all conn))

;; 一覧
(core/all-customer-names (d/db conn))
(core/all-book-titles (d/db conn))
(core/all-invitations (d/db conn))

;; カスタマーを追加
(core/invite-customer
  conn
  "深川125"
  {:name "お得意様"
   :pref :customer.pref/Fukuoka})

;; レビューを登録
(core/review-book
  conn
  "深川125"
  "プログラミングClojure"
  {:title "オススメ"
   :num-stars 5})
(core/review-book
  conn
  "ただのカスタマー"
  "プログラミングClojure"
  {:title "日本語で読めるClojure入門"
   :num-stars 4})

;; レビュー一覧
(core/all-reviews (d/db conn) "プログラミングClojure")
