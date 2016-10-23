(require '[datomic.api :as d])

(def uri-in-memory "datomic:mem://hello")
(def uri-local-disk "datomic:free://localhost:4334/hello")

(d/delete-database uri-in-memory)
(d/create-database uri-in-memory)
(def conn (d/connect uri-in-memory))

(def db1 (d/db conn))

(d/transact
  conn
  [[:db/add                       ;; 事実の表明:
    #db/id [:db.part/user]        ;;   新規エンティティの、
    :db/doc "Hello, world!"]])    ;;   :db/doc属性の値は"Hello, world!"である。

(d/q
  '[:find ?e ?tx
    :where
    [?e :db/doc "Hello, world!" ?tx]]
  db1)          ;; => #{}

(d/q
  '[:find ?e ?tx
    :where
    [?e :db/doc "Hello, world!" ?tx]]
  (d/db conn))  ;; => #{[17592186045417 13194139534312]}など
