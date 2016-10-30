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
       [(= ?ns "db.type")]]
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

;; :customer/pref属性用の値。
(d/q '[:find [?pref ...]
       :where
       [_ :db/ident ?pref]
       [(namespace ?pref) ?ns]
       [(= ?ns "customer.pref")]]
     (d/db conn))
;; => [:customer.pref/Fukuoka :customer.pref/Okayama :customer.pref/Hiroshima
;;     :customer.pref/Yamaguchi :customer.pref/Shimane :customer.pref/Tottori]

;; カスタマーエンティティを追加。
(d/transact
  conn
  [{:db/id #db/id [:db.part/user -1]
    :customer/name "深川125"
    :customer/pref :customer.pref/Hiroshima}
   {:db/id #db/id [:db.part/user -2]
    :customer/name "とるたん"
    :customer/invited-by #db/id [:db.part/user -1]
    :customer/pref :customer.pref/Yamaguchi}
   {:db/id #db/id [:db.part/user]
    :customer/name "お得意様"
    :customer/invited-by #db/id [:db.part/user -1]
    :customer/pref :customer.pref/Fukuoka}])

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




(defn attrs-in-ns
  "nspace名前空間の属性一覧。"
  [db nspace]
  (d/q '[:find [?aname ...]
         :in $ ?nspace
         :where
         [:db.part/db :db.install/attribute ?a]
         [?a :db/ident ?aname]
         [(namespace ?aname) ?ns]
         [(= ?ns ?nspace)]]
       db nspace))

(attrs-in-ns (d/db conn) "book")    ;; => [:book/title :book/price :book/reviews]
(attrs-in-ns (d/db conn) "review")  ;; => [:review/title :review/num-stars :review/reviewer]

;; 書籍とレビューを追加。
(d/transact
  conn
  [{:db/id #db/id [:db.part/user]
    :book/title "プログラミングClojure"
    :book/price 3672
    :book/reviews [{:review/title "オススメ"
                    :review/num-stars 5
                    :review/reviewer [:customer/name "深川125"]}
                   {:review/title "定番"
                    :review/num-stars 4
                    :review/reviewer [:customer/name "とるたん"]}]}
   {:db/id #db/id [:db.part/user]
    :book/title "Land of Lisp"
    :book/price 4104
    :book/reviews [{:review/title "最高"
                    :review/num-stars 5
                    :review/reviewer [:customer/name "深川125"]}]}])

;; 「プログラミングClojure」の価格とレビュータイトル。
(d/q '[:find ?p ?rt
       :where
       [?e :book/title "プログラミングClojure"]
       [?e :book/price ?p]
       [?e :book/reviews ?r]
       [?r :review/title ?rt]]
     (d/db conn)) ;; => #{[3672 "定番"] [3672 "オススメ"]}

;; 価格を改訂。
(d/transact
  conn
  [{:db/id #db/id [:db.part/user]       ;; 新規エンティティの追加のように見えるが、
    :book/title "プログラミングClojure" ;; :book/titleの:db/unique属性は:db.unique/identityなので、
    :book/price 3200}                   ;; 仮IDは、既存エンティティのIDに解決される。
   {:db/id #db/id [:db.part/user]   ;; 一方、こちらは同名の既存エンティティが無いので、
    :book/title "On Lisp"           ;; 新規エンティティになる。
    :book/price 4104}])             ;; 新規か更新かに関係なく、同じtx-dataの書き方で良い、というのがミソ。

;; 全書籍のタイトルと価格。
(d/q '[:find ?t ?p
       :where
       [?e :book/title ?t]
       [?e :book/price ?p]]
     (d/db conn)) ;; => #{["プログラミングClojure" 3200] ["Land of Lisp" 4104] ["On Lisp" 4104]}
