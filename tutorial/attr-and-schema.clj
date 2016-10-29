(require '[datomic.api :as d]
         '[datom-sand.util :as util])

(def uri-in-memory "datomic:mem://hello")

(d/delete-database uri-in-memory)
(d/create-database uri-in-memory)
(def conn (d/connect uri-in-memory))

;; 属性一覧。
(d/q '[:find [?aname ...]
       :where
       [:db.part/db :db.install/attribute ?a]
       [?a :db/ident ?aname]]
     (d/db conn)) ;; => [:db/code :db.sys/reId :db/doc ...]

;; :db/ident属性の属性。
(d/q '[:find [?aname ...]
       :where
       [:db/ident ?a]
       [?a :db/ident ?aname]]
     (d/db conn)) ;; => [:db/doc :db/valueType :db/unique :db/ident :db/cardinality]

;; :db/valueType属性に使える値。
(d/q '[:find [?vt ...]
       :where
       [_ :db/ident ?vt]
       [(namespace ?vt) ?ns]
       [(name ?vt) ?nm]
       [(re-matches #"^db.type" ?ns)]]
     (d/db conn))
;; => [:db.type/instant :db.type/uri :db.type/ref :db.type/keyword :db.type/bytes
;;     :db.type/fn :db.type/bigdec :db.type/long :db.type/uuid :db.type/bigint :db.type/float
;;     :db.type/boolean :db.type/string :db.type/double]
