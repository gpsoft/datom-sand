(require '[datomic.api :as d]
         '[datom-sand.util :as util])

(def uri-in-memory "datomic:mem://hello")

(d/delete-database uri-in-memory)
(d/create-database uri-in-memory)
(def conn (d/connect uri-in-memory))

;; Sample schema (Don't worry about it for now)
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

;; Sample entities
(def e32 (util/mk-entity conn "Entity#32"))
(def e33 (util/mk-entity conn "Entity#33"))
(def e34 (util/mk-entity conn "Entity#34"))

;; factの表明
(def tx-data1
  [[:db/add e32 :live-in "Japan"]         ;; e32は日本に住む
   [:db/add e32 :like "Programming"]      ;; e32はプログラミングが好きだ
   [:db/add e32 :work-for "Panasonic"]])  ;; e32はPanasonicで働いている
(d/transact conn tx-data1)

(util/entity (d/db conn) e32 :work-for)   ;; => "Panasonic"

;; factの撤回
(def tx-data2
  [[:db/retract e32 :work-for "Panasonic"]])
(d/transact conn tx-data2)

(util/entity (d/db conn) e32 :work-for)   ;; => nil

;; tx-dataのマップ表記
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

;; 仮IDと新規エンティティ
(def tx-data4
  [{:db/id #db/id [:db.part/user]
    :live-in "Canada"
    :like ["Sleeping" "Eating" "Watching TV"]}
   [:db/add #db/id [:db.part/user -1] :live-in "France"]
   [:db/add #db/id [:db.part/user -1] :work-for "Interpol"]])
(d/transact conn tx-data4)    ;; 2つのエンティティを追加

;; 仮IDの解決(resolve)
(let [tmp1 (d/tempid :db.part/user -1)
      tmp2 (d/tempid :db.part/user -2)
      tx-data [{:db/id tmp1
                :live-in "Canada"
                :like ["Sleeping" "Eating" "Watching TV"]}
               [:db/add tmp2 :live-in "France"]
               [:db/add tmp2 :work-for "Interpol"]]
      {:keys [db-after tempids]} @(d/transact conn tx-data)]
  [(d/resolve-tempid db-after tempids tmp1)
   (d/resolve-tempid db-after tempids tmp2)]) ;; => [17592186045421 17592186045422]など

;; 独自のパーティション
(d/transact
  conn
  [{:db/id #db/id [:db.part/db]
    :db/ident :my-part
    :db.install/_partition :db.part/db}])
