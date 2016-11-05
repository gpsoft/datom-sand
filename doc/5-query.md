# 5. クエリー

## Datomicのクエリー

- 論理プログラミングを基礎にしたクエリー言語[Datalog](https://en.wikipedia.org/wiki/Datalog)を、Clojure文法に落とし込んだもの
- 書式

          [:find ?a ?b             ;; SELECT句に相当。
           :in $db1 $db2 ?c ?d     ;; 仮引数。
           :where                  ;; WHERE句に相当。
           data1
           data2
           data3
           ...]
          ;; 他に:withもある(集約で使う)。

  - ベクタで記述
  - `?a`, `?b`, `?c` ...変数
  - `$db1`, `$db2` ...DBのスナップショット
    - DBスナップショットが1つだけなら、単に`$`とする
  - `data1`, `data2`, `data3` ...Datomのパターン
  - `$`以外の仮引数が無いなら`:in`は省略可能
- 例

          ;; 書籍タイトルで検索するクエリー式。
          (def find-books-by-title
            '[:find [?bt ...]
              :in $ ?regex
              :where
              [?e :book/title ?bt _]
              [(re-matches ?regex ?bt)]])

  - Where句(`:where`)で、Datom(EAVT)をパターンマッチさせる
    - `[?e :book/title ?bt _]` ...Eが`?e`、Aが`:book/title`、Vが`?bt`、Tがdon't care
    - `[?e :book/title ?bt]`と書いても良い
  - あるいはS式で条件を記述
    - `[(re-matches ?regex ?bt)]` ...`?bt`が正規表現`?regex`にマッチする

- クエリーを実行するには、`d/q`関数か`d/query`関数を使用

          ;; タイトルに"Lisp"を含む書籍。
          (d/q find-books-by-title
               db
               #".*Lisp.*")  ;; => ["Land of Lisp" "On Lisp"]

          (d/query {:query find-books-by-title
                    :args [db #".*Lisp.*"]
                    :timeout 1000}) ;; 1000msec

  - 実引数は、`db`と`#".*Lisp.*"`
  - それぞれ、クエリー式の仮引数`$`と`regex`にバインドされる
  - `d/query`はタイムアウトを指定可能

- クエリー式は、マップや文字列で記述することも可能

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

#### さらに例

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

## Find句(`:find`)

- 基本は4パターン
  - `:find ?a ?b ?c` ...`#{[a b c] [aa bb cc] [aaa bbb ccc]}`(セットで返る)
  - `:find [?a ...]` ...`[a aa aaa]`(ベクタで返る)
  - `:find ?a .` ...`a`(1つだけ返る)
  - `:find [?a ?b ?c]` ... `[a b c]`(1組だけ返る)

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

## 集約とWith句(`:with`)

- 集約も可能

          ;; 各書籍のレビュー星数の平均。
          (d/q '[:find ?bt (avg ?stars)
                 :where
                 [?e :book/title ?bt]
                 [?e :book/reviews ?r]
                 [?r :review/num-stars ?stars]]
               db)
          ;; => [["Land of Lisp" 5.0]
          ;;     ["On Lisp" 4.5]
          ;;     ["Real World Haskell" 3.5]
          ;;     ["すごいHaskellたのしく学ぼう!" 4.0]
          ;;     ["プログラミングClojure" 4.5]]

  - ただし、↑これは間違い
    - `[?bt ?stars]`のセットに対して集約が行われるので、重複が除外されてしまう
    - 例えば「プログラミングClojure」には6つのレビューがあり、星数は5, 5, 4, 5, 4, 5だが、↑ここでは5と4の平均が出ている
  - `:with`により、集約対象を明示すれば良い

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

##### 集約関数

    ;; スカラ値を返すもの。
    min, max,
    count, count-distinct,
    sum, avg,
    median, variance, stddev,
    rand

    ;; コレクションを返すもの。
    (distinct ?xs)  ;; 重複を除去。
    (min 5 ?xs)     ;; 下位5つ。
    (max 5 ?xs)     ;; 上位5つ。
    (rand 5 ?xs)    ;; ランダムに5つ(同じものが選ばれる可能性あり)。
    (sample 5 ?xs)  ;; 最大5つのdistinctな値。

##### 集約関数の自作

    (defn longest-title [titles]
      (let [longest (apply max-key count titles)]
        (str longest "(" (count longest) ")")))

    (d/q '[:find [(user/longest-title ?bt) ...] ;; namespace付きで使用。
           :where
           [?e :book/title ?bt]]
         db)  ;; => ["Real World Haskell(18)"]

## In句(`:in`)

- `d/q`や`d/query`で渡す引数を`:in`で受け取る
- In句には記号(`$`,`?`,`%`)を前置したシンボルを列挙

             :in $db1 $db2 ?c ?d %

- `$`で始まるのはDBスナップショット
  - Where句で使用する

            :where
            [$db1 ?e :customer/name ?n]
            [$db2 ?ee :book/title ?bt]

  - DBスナップショットが1つだけなら、単に`$`として良い
    - その場合、Where句に書く必要もない

              :in $ ?c ?d
              :where
              [?e ?a ?v ?t]

- `?`で始まるのは変数
  - Where句の中で、パターンマッチに使用する
- `%`は、Where句の中でルールを使う場合に指定する(ルールについては後述)
- `$`以外の入力が無い場合、`:in`は省略可能

## In句の変数バインディング

- クエリーへの入力をIn句の変数へバインドする、4つのパターン
  - スカラ

            ;; クエリ式
            :in $ ?a

            ;; クエリ実行
            (d/q '[……]
              db
              "val")

            ;; バインド
            ?a ← "val"

  - 組

            ;; クエリ式
            :in $ [?a ?b]

            ;; クエリ実行
            (d/q '[………]
              db
              ["vala" "valb"])

            ;; バインド
            ?a ← "vala" かつ ?b ← "valb"

  - コレクション

            ;; クエリ式
            :in $ [?a ...]

            ;; クエリ実行
            (d/q '[………]
              db
              ["vala1" "vala2" "vala3"])

            ;; バインド
            ?a ← "vala1" または ?a ← "vala2" または ?a ← "vala3"

  - 関係

            ;; クエリ式
            :in $ [[?a ?b]]

            ;; クエリ実行
            (d/q '[………]
              db
              [["vala1" "valb1"]
               ["vala2" "valb2"]])

            ;; バインド
            ?a ← "vala1" かつ ?b ← "valb1"
            または
            ?a ← "vala2" かつ ?b ← "valb2"

#### 例

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

## What's next?
- [コードを見る](../tutorial/query.clj)
- [4. 属性とスキーマ](4-attr-and-schema.md)
- [6. Pull](6-pull.md)
