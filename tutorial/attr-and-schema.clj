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




;; schema.ednのすべてのtx-dataをassert。
(->> (util/read-edn "datom-sand/schema.edn")
     (util/tx-all conn))

;; customer名前空間の属性一覧。
(d/q '[:find [?aname ...]
       :where
       [:db.part/db :db.install/attribute ?a]
       [?a :db/ident ?aname]
       [(namespace ?aname) ?ns]
       [(= ?ns "customer")]]
     (d/db conn)) ;; => [:customer/name :customer/pref :customer/invited-by :customer/likes]

;; カスタマーエンティティを追加。
(d/transact
  conn
  [{:db/id #db/id [:db.part/user -1]
    :customer/name "深川125"}
   {:db/id #db/id [:db.part/user -2]
    :customer/name "とるたん"
    :customer/invited-by #db/id [:db.part/user -1]}
   {:db/id #db/id [:db.part/user]
    :customer/name "お得意様"
    :customer/invited-by #db/id [:db.part/user -1]}])

;; とるたんを紹介したカスタマー。
(d/q '[:find [?invitor-name ...]
       :where
       [?e :customer/name "とるたん"]
       [?e :customer/invited-by ?invitor]
       [?invitor :customer/name ?invitor-name]]
     (d/db conn)) ;; => ["深川125"]

;; 深川125が紹介したカスタマー(正攻法)。
(d/q '[:find [?invitee-name ...]
       :where
       [?e :customer/name "深川125"]
       [?invitee :customer/invited-by ?e]   ;; ここで[?e :customer/_invited-by ?invitee]とは書けない。
       [?invitee :customer/name ?invitee-name]]
     (d/db conn)) ;; => ["お得意様" "とるたん"]

;; 深川125が紹介したカスタマー(逆方向の参照を使う)。
(->> [:customer/name "深川125"]   ;; Lookup Ref
     (d/entity (d/db conn))       ;; Entityオブジェクトを取得。
     :customer/_invited-by
     seq
     (map :customer/name))  ;; => ("とるたん" "お得意様")

