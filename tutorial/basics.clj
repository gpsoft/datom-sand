(require '[datomic.api :as d])

;; URI
(def uri-in-memory "datomic:mem://hello")
(def uri-local-disk "datomic:free://localhost:4334/hello")

;; 空っぽのDBを作り、接続。
(d/delete-database uri-in-memory)
(d/create-database uri-in-memory)
(def conn (d/connect uri-in-memory))

;; そのスナップショット。
(def db1 (d/db conn))

;; Datomを1つassert。
(d/transact
  conn
  [[:db/add                       ;; factの表明:
    #db/id [:db.part/user]        ;;   新規エンティティの、
    :db/doc "Hello, world!"]])    ;;   :db/doc属性の値は"Hello, world!"である。

;; クエリー。
(d/q
  '[:find ?e ?tx
    :where
    [?e :db/doc "Hello, world!" ?tx]]
  db1)          ;; => #{}

;; 最新のスナップショット。
(def db2 (d/db conn))

;; 再クエリー。
(d/q
  '[:find ?e ?tx
    :where
    [?e :db/doc "Hello, world!" ?tx]]
  db2)  ;; => #{[17592186045417 13194139534312]}など、ベクタのセットを返す。

;; さらにassert。
(d/transact
  conn
  [[:db/add #db/id [:db.part/user] :db/doc "Hello, Datomic!"]
   [:db/add #db/id [:db.part/user] :db/doc "Hello, Clojure!"]])

;; 最新のスナップショット。
(def db3 (d/db conn))

;; :db/doc属性の値が"Hello"で始まる、すべてのエンティティを探す。
(def all-hello-docs
  '[:find ?e ?doc
    :where
    [?e :db/doc ?doc]                   ;; For now,
    [(re-matches #"^Hello.+$" ?doc)]])  ;; just imagine what's going on here.

(count (d/q all-hello-docs db1))  ;; => 0
(count (d/q all-hello-docs db2))  ;; => 1
(count (d/q all-hello-docs db3))  ;; => 3

;; :db/doc属性値のみ取り出す。
(->> (d/q all-hello-docs db3)
     seq
     (map second))  ;; => ("Hello, Clojure!" "Hello, Datomic!" "Hello, world!")
