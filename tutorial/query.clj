(require '[datomic.api :as d]
         '[datom-sand.util :as util])

;; 空っぽのDBを作り、
;; スキーマとサンプルDatomをassert。
(def conn (util/empty-conn))
(->> (util/read-edn "datom-sand/schema.edn")
     (util/tx-all conn))
(->> (util/read-edn "datom-sand/customers.edn")
     (util/tx-all conn))
(->> (util/read-edn "datom-sand/books.edn")
     (util/tx-all conn))

(def db (d/db conn))

;; 書籍タイトルで検索するクエリー式。
(def find-books-by-title
  '[:find [?bt ...]
    :in $ ?regex
    :where
    [?e :book/title ?bt]
    [(re-matches ?regex ?bt)]])

;; タイトルに"Lisp"を含む書籍。
(d/q find-books-by-title
     db
     #".*Lisp.*")  ;; => ["Land of Lisp" "On Lisp"]
(d/query {:query find-books-by-title
          :args [db #".*Lisp.*"]
          :timeout 1000}) ;; 1000msec

;; クエリーをマップや文字列で記述。
(def query-m
  '{:find [[?bt ...]]
    :in [$ ?regex]
    :where [[?e :book/title ?bt]
            [(re-matches ?regex ?bt)]]})
(def query-s
  (str 
    "[:find [?bt ...]"
    " :in $ ?regex"
    " :where"
    " [?e :book/title ?bt]"
    " [(re-matches ?regex ?bt)]]"))
(d/q query-m db #".*Clojure")   ;; => ["プログラミングClojure"]
(d/q query-s db #".*Haskell.*") ;; => ["すごいHaskellたのしく学ぼう!" "Real World Haskell"]


;; 「深川125」がレビューした書籍。
(d/q
  '[:find [?bt ...]
    :where
    [?r :review/reviewer [:customer/name "深川125"]]  ;; Lookup Ref
    [?e :book/reviews ?r]
    [?e :book/title ?bt]]
  db)   ;; => ["Land of Lisp" "On Lisp" "プログラミングClojure" "Real World Haskell"]

;; 星3つ以下のレビューが付いた書籍とレビューア名。
(d/q
  '[:find ?bt ?n
    :where
    [?e :book/title ?bt]
    [?e :book/reviews ?r]
    [?r :review/num-stars ?stars]
    [?r :review/reviewer ?reviewer]
    [?reviewer :customer/name ?n]
    [(< ?stars 4)]]
  db) ;; => #{["Real World Haskell" "お得意様"]}


;; Find句の記法。
(d/q '[:find ?e ?bt
       :where [?e :book/title ?bt]] db)
;; => #{[17592186045442 "Real World Haskell"]
;;      [17592186045455 "すごいHaskellたのしく学ぼう!"]
;;      [17592186045446 "Land of Lisp"]
;;      [17592186045450 "On Lisp"]
;;      [17592186045435 "プログラミングClojure"]}
(d/q '[:find [?bt ...]
       :where [?e :book/title ?bt]] db)
;; => ["Land of Lisp"
;;     "すごいHaskellたのしく学ぼう!"
;;     "On Lisp" "プログラミングClojure"
;;     "Real World Haskell"]
(d/q '[:find ?bt .
       :where [?e :book/title ?bt]] db)
;; => "Land of Lisp"
(d/q '[:find [?e ?bt]
       :where [?e :book/title ?bt]] db)
;; => [17592186045442 "Real World Haskell"]


;; 集約
(d/q '[:find ?bt (avg ?stars)
       :where
       [?e :book/title ?bt]
       [?e :book/reviews ?r]
       [?r :review/num-stars ?stars]]
     db)
;; => [...
;;     ...
;;     ["プログラミングClojure" 4.5]]
;; ↑これは間違い。

(d/q '[:find ?rt ?stars
       :where
       [?e :book/title "プログラミングClojure"]
       [?e :book/reviews ?r]
       [?r :review/num-stars ?stars]
       [?r :review/title ?rt]]
     db)
;; => #{["Lispの一種" 5]
;;      ["キーパーソンStuの本" 5]
;;      ["定番" 4]
;;      ["和書" 5]
;;      ["関数型プログラミング" 4]
;;      ["オススメ" 5]}
;; 平均は...
(-> (apply + [5 5 4 5 4 5])
    (/ 6)
    double)  ;; => 4.666666666666667

;; :withにより、集約対象をコントロール。
(d/q '[:find ?bt (avg ?stars)
       :with ?r
       :where
       [?e :book/title ?bt]
       [?e :book/reviews ?r]
       [?r :review/num-stars ?stars]]
     db)
;; => [["Land of Lisp" 5.0]
;;     ["On Lisp" 4.5]
;;     ["Real World Haskell" 3.6666666666666665]
;;     ["すごいHaskellたのしく学ぼう!" 4.0]
;;     ["プログラミングClojure" 4.666666666666667]]

;; count, min, max
(d/q '[:find ?bt (count ?stars) (min ?stars) (max ?stars)
       :with ?r
       :where
       [?e :book/title ?bt]
       [?e :book/reviews ?r]
       [?r :review/num-stars ?stars]]
     db)
;; => [["Land of Lisp" 3 5 5]
;;     ["On Lisp" 4 4 5]
;;     ["Real World Haskell" 3 3 4]
;;     ["すごいHaskellたのしく学ぼう!" 2 4 4]
;;     ["プログラミングClojure" 6 4 5]]

;; 集約関数の自作。
(defn longest-title [titles]
  (let [longest (apply max-key count titles)]
    (str longest "(" (count longest) ")")))
(d/q '[:find [(user/longest-title ?bt) ...] ;; namespace付きで使用。
       :where
       [?e :book/title ?bt]]
     db)  ;; => ["Real World Haskell(18)"]

;; 「深川125」がレビューしたClojureの書籍か、
;; 「ドラゴン」がレビューしたHaskellの書籍。
(d/q '[:find ?name ?bt
       :in $ [[?name ?regex]]
       :where
       [?e :customer/name ?name]
       [?r :review/reviewer ?e]
       [?b :book/reviews ?r]
       [?b :book/title ?bt]
       [(re-matches ?regex ?bt)]]
     db [["深川125" #".*Clojure.*"]
         ["ドラゴン" #".*Haskell.*"]])
;; => #{["ドラゴン" "すごいHaskellたのしく学ぼう!"]
;;      ["深川125" "プログラミングClojure"]
;;      ["ドラゴン" "Real World Haskell"]}


;; `:customer/*`属性。
(d/q '[:find [?an ...]
       :where
       [:db.part/db :db.install/attribute ?a]
       [?a :db/ident ?an]
       [(namespace ?an) ?ns]
       [(= ?ns "customer")]]
     db)
;; => [:customer/name
;;     :customer/pref
;;     :customer/invited-by
;;     :customer/likes]

;; タイトルが"p"で終わる書籍。
(d/q '[:find [?bt ...]
       :where
       [?e :book/title ?bt]
       [(.endsWith ?bt "p")]]
     db)  ;; => ["Land of Lisp" "On Lisp"]

;; DBスナップショットなしでクエリする
(d/q '[:find ?k ?v
       :where
       [(System/getProperties) [[?k ?v]]]
       [(.startsWith ^String ?k "java.vm")]])
;; => #{["java.vm.version" "25.31-b07"]
;;      ["java.vm.specification.version" "1.8"]
;;      ["java.vm.vendor" "Oracle Corporation"]
;;      ...}
(d/q '[:find ?e ?a ?v ?t
       :in [[?e ?a ?v ?t]]
       :where
       [(= ?t 101)]]
     [[1 :attr "hoge" 101]
      [2 :attr "fuga" 101]
      [3 :attr "piyo" 102]
      [4 :attr "piyopiyo" 103]])
;; => #{[1 :attr "hoge" 101]
;;      [2 :attr "fuga" 101]}
