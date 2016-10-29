(require '[datomic.api :as d]
         '[datom-sand.util :as util])

(def uri-in-memory "datomic:mem://hello")

(d/delete-database uri-in-memory)
(d/create-database uri-in-memory)
(def conn (d/connect uri-in-memory))

;; 準備(Don't worry about it for now)。
;; サンプルのスキーマとエンティティ(e32, e33, e34)をassert。
(d/transact
  conn
  [{:db/id #db/id [:db.part/db]
    :db/ident :live-in
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}
   {:db/id #db/id [:db.part/db]
    :db/ident :like
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db.install/_attribute :db.part/db}
   {:db/id #db/id [:db.part/db]
    :db/ident :work-for
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}])
(def e32 (util/mk-entity conn "Entity#32"))
(def e33 (util/mk-entity conn "Entity#33"))
(def e34 (util/mk-entity conn "Entity#34"))




;; 表明用Datom。
(def tx-data1
  [[:db/add e32 :live-in "Japan"]         ;; e32は日本に住む。
   [:db/add e32 :like "Programming"]      ;; e32はプログラミングが好きだ。
   [:db/add e32 :work-for "Panasonic"]])  ;; e32はPanasonicで働いている。
(d/transact conn tx-data1)

;; e32の:work-for属性値を得る。
(util/entity (d/db conn) e32 :work-for)   ;; => "Panasonic"


;; 撤回用Datom。
(def tx-data2
  [[:db/retract e32 :work-for "Panasonic"]])
(d/transact conn tx-data2)                ;; もはや、Panasonicでは働いてない。

(util/entity (d/db conn) e32 :work-for)   ;; => nil


;; tx-dataのマップ表記。
(def tx-data3
  [{:db/id e33
    :live-in "Canada"
    :like ["Sleeping" "Eating" "Watching TV"]}
   {:db/id e34
    :live-in "France"
    :work-for "Interpol"}])
(d/transact conn tx-data3)

(util/entity (d/db conn) e33 :like)       ;; => #{"Sleeping" "Eating" "Watching TV"}
(util/entity (d/db conn) e34 :live-in)    ;; => "France"


;; tx-data3をassertしたときの
;; トランザクションエンティティ。
(d/q '[:find ?instant ?tx
       :in $ ?e
       :where
       [?e :live-in "France" ?tx]
       [?tx :db/txInstant ?instant]]
     (d/db conn) e34) ;; => #{[#inst "2016-10-29T03:25:27.828-00:00" 13194139534321]}

;; e32に関するトランザクションの数。
;; 撤回用Datomはカウントされない。
;; d/qでは、撤回用Datomを見ることはできない。
(count (d/q '[:find ?tx
              :in $ ?e
              :where
              [?e _ _ ?tx]]
            (d/db conn) e32)) ;; => 2(エンティティ作成時と:live-inなどをassertしたとき)

;; 仮IDと新規エンティティ。
;; 2つのエンティティを追加するtx-data。
(def tx-data4
  [{:db/id #db/id [:db.part/user]
    :live-in "Mexico"
    :like ["Taco" "Burrito" "Empanada"]}
   [:db/add #db/id [:db.part/user -1] :live-in "Italy"]
   [:db/add #db/id [:db.part/user -1] :work-for "Ferrari"]])

;; Tx後に、仮IDから正式なエンティティIDを得る。
;;   =>仮IDの解決(resolve)
(let [tmp1 (d/tempid :db.part/user -1)
      tmp2 (d/tempid :db.part/user -2)
      tx-data [{:db/id tmp1
                :live-in "Mexico"
                :like ["Taco" "Burrito" "Empanada"]}
               [:db/add tmp2 :live-in "Italy"]
               [:db/add tmp2 :work-for "Ferrari"]]
      {:keys [db-after tempids]} @(d/transact conn tx-data)]
  [(d/resolve-tempid db-after tempids tmp1)
   (d/resolve-tempid db-after tempids tmp2)]) ;; => [17592186045421 17592186045422]など。

;; 住んでる国。
(d/q '[:find [?c ...]
       :where
       [_ :live-in ?c]]
     (d/db conn)) ;; => ["Canada" "Japan" "Mexico" "Italy" "France"]

;; 好きなもの。
(->>
  (d/q '[:find ?e ?f
         :where
         [?e :like ?f]]
       (d/db conn))
  seq
  (sort-by first))
;; => ([17592186045418 "Programming"]
;;     [17592186045420 "Eating"]
;;     [17592186045420 "Sleeping"]
;;     [17592186045420 "Watching TV"]
;;     [17592186045427 "Empanada"]
;;     [17592186045427 "Taco"]
;;     [17592186045427 "Burrito"])


;; 独自のパーティション。
(d/transact
  conn
  [{:db/id #db/id [:db.part/db]
    :db/ident :my-part
    :db.install/_partition :db.part/db}])

;; パーティション一覧。
(d/q '[:find [?pname ...]
       :where
       [:db.part/db :db.install/partition ?p]
       [?p :db/ident ?pname]]
     (d/db conn)) ;; => [:db.part/db :db.part/tx :my-part :db.part/user]
