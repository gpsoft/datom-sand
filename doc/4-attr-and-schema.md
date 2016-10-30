# 4. 属性とスキーマ

## 属性(Attribute)

- 属性はDatomの構成要素の1つ(EAVTのA)であり、スキーマを決める
- 属性も、一種のエンティティ
- エンティティなので、属性を持てる ...属性エンティティの属性
- 組み込みの、属性エンティティ用の属性エンティティ
  - 属性エンティティの必須属性:
    - `:db/ident` ...別名
    - `:db/valueType` ...型
    - `:db/cardinality` ...多重度
  - 属性エンティティによく付けられる任意の属性:
    - `:db/doc` ...説明
    - `:db/unique` ...一意性
    - `:db/isComponent` ...コンポーネントかどうか
    - `:db/noHistory` ...履歴管理するかどうか
    - `:db/index` ...インデックス有無
    - `:db/fulltext` ...全文検索対象かどうか
  - :db/identと:db/docを除き、これらは、属性エンティティ専用の属性エンティティ

##### 属性エンティティの別名の例

    :comments
    :book/title
    :customer/name
    :customer/pref
    :customer/invited-by

- 通常、namespaceの部分(`book`や`customer`)により、属性をグループ化する
- 「namespace＝エンティティ名」とすることが多いが、そうしないことも珍しくない

##### 組み込みの属性エンティティの例(上記以外)

    :db.install/partition
    :db.install/attribute
    :db.alter/attribute
    :db.install/valueType
    :db.install/function
    :db/txInstant
    :db/code
    :db/fn
    :db/lang

## スキーマ(Schema)

- スキーマを決めることの本質は
  - 属性の性質(その属性が、どんな値を取りうるか)を定義することであり
  - あるエンティティが、どんな属性を持ちうるかを定義することではない(Datomicでは、それは自由)
- エンティティと属性の組み合わせに制約がないので、RDBのスキーマより自由度が高い
- 属性もエンティティの一種なので、スキーマを定義するには、その属性エンティティに関するfactを、トランザクションにより記録すればよい
- ただし、いくつか決まり事がある
  - 属性エンティティは、`:db.part/db`パーティションに配置する
  - 属性エンティティには、`:db/ident`、`:db/valueType`、`:db/cardinality`属性が必須
  - 定義する属性エンティティを、`:db.part/db`エンティティの、`:db.install/attribute`属性の値として関連付ける

##### 属性を定義するtx-dataの例

    [;; (1):customer/nameという名前(別名)で、
     ;; (2)文字列型で、
     ;; (3)多重度1の、
     ;; (4)「カスタマーの名前」として使うための
     ;; (5)属性エンティティ。
     [:db/add     ;;(1)
      #db/id [:db.part/db -1]
      :db/ident :customer/name]
     [:db/add     ;;(2)
      #db/id [:db.part/db -1]
      :db/valueType :db.type/string]
     [:db/add     ;;(3)
      #db/id [:db.part/db -1]
      :db/cardinality :db.cardinality/one]
     [:db/add     ;;(4)
      #db/id [:db.part/db -1]
      :db/doc "カスタマーの名前"]
     [:db/add     ;;(5)
      #db/id [:db.part/db -1]
      :db.install/_attribute :db.part/db]]

- `:db.type/string`や`:db.cardinality/one`は、属性値として使うために定義された組み込みのエンティティの別名
- 属性は特殊なエンティティなので、DBに、属性用のエンティティだと認識させる必要がある
  - 前述の5番目のDatomは、そのためのもの

            [:db/add     ;;(5)
             #db/id [:db.part/db -1]
             :db.install/_attribute :db.part/db]

    - 属性名に`_`を前置すると、関連付けの方向が逆転する
    - 仮に、この新しい属性エンティティのIDが85だとすると、以下のDatomと同じこと

              [:db/add :db.part/db :db.install/attribute 85]

## 属性の型(`:db/valueType`属性の値)

    :db.type/string
    :db.type/boolean
    :db.type/long
    :db.type/double
    :db.type/ref
    :db.type/fn
    ...

## 参照型(`:db.type/ref`)

- 参照型の属性は、別のエンティティを参照するための属性
- RDBのforeign keyに近いイメージ

          [{:db/id #db/id [:db.part/db]
            :db/ident :customer/invited-by
            :db/doc "カスタマーが、誰に紹介されたか"
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/one
            :db.install/_attribute :db.part/db}]

- Datomicの参照型は、自動的に双方向になる
- 別名に`_`を付けると逆方向の意味
  - あるカスタマーの`:customer/invited-by`属性を見れば、そのカスタマーが誰から紹介されたかが分かる
  - 一方、カスタマーの`:customer/_invited-by`属性を見れば、そのカスタマーが誰を紹介したかが分かる

## Enum型

- 「Enum型」というのは無いが、参照型で代用可能
- 属性値になりうるエンティティを、別名付きで、あらかじめ表明しておく

          ;; Enum型の属性
          [{:db/id #db/id [:db.part/db]
            :db/ident :customer/pref
            :db/doc "カスタマーが、どこに住んでるか(都道府県)"
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/one
            :db.install/_attribute :db.part/db}]

          ;; :customer/pref属性の属性値用のエンティティ
          [[:db/add #db/id[:db.part/user]
            :db/ident :customer.pref/Hiroshima]
           [:db/add #db/id[:db.part/user]
            :db/ident :customer.pref/Yamaguchi]
           [:db/add #db/id[:db.part/user]
            :db/ident :customer.pref/Fukuoka]
           ...]

          ;; カスタマーエンティティ
          [{:db/id #db/id [:db.part/user]
            :customer/name "深川125"
            :customer/pref :customer.pref/Hiroshima}
           ...]

## 多重度(`:db/cardinality`属性)

- 多重度の値は、`:db.cardinality/one`か`:db.cardinality/many`のどちらか

          [{:db/id #db/id [:db.part/db]
            :db/ident :customer/likes
            :db/doc "カスタマーが好きなもの"
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/many
            :db.install/_attribute :db.part/db}]

  - `many`なら、1つのエンティティに複数の属性値を関連付けることができる

            ;; 1つずつでもいいし、
            [[:db/add customer1 :customer/likes review1]
             [:db/add customer1 :customer/likes comment30]
             [:db/add customer1 :customer/likes comment31]]
            ;; まとめてもいい。
            [[:db/add customer1
              :customer/likes [review1 comment30 comment31]]]

  - `one`なら、あと勝ち(古いDatomは自動的に`:db/retract`される

## 一意性(`:db/unique`)

- 属性エンティティに`:db/unique`属性を付けると、その属性値はシステム内で一意となる

          [{:db/id #db/id [:db.part/db]
            :db/ident :book/title
            :db/doc "書籍のタイトル"
            :db/valueType :db.type/string
            :db/cardinality :db.cardinality/one
            :db/unique :db.unique/identity
            :db.install/_attribute :db.part/db}]

  - このドメインでは、同じタイトルの書籍が複数存在することは無い
    - 既存の複数のエンティティに、同じ`:book/title`属性値を関連付けようとすると、例外が発生する

### Upsert(Insert or Update)

- `:db/unique`属性の値は、`:db.unique/identity`か`:db.unique/value`のどちらか
- `identity`と`value`の違いは、仮エンティティIDの解決方法

          ;; 既に「プログラミングClojure」が登録済みで、
          ;; そのエンティティIDが63と仮定する。
          ;; ここで、下記のtx-dataをトランザクションすると…
          [{:db/id #db/id [:db.part/user]
            :book/title "プログラミングClojure"
            :book/price 3200}]

  - `:book/title`の`:db/unique`属性値が`identity`なら仮エンティティIDは63に解決され、`value`なら例外が発生する
  - もし`:db/unique`属性を持たないなら、普通通り、新規のエンティティIDに解決される
  - `identity`の場合、上記のtx-dataは:
    - もし「プログラミングClojure」が存在しないなら新規に作り(Insert)、
    - 存在するなら更新しろ(Update)、という意味になる
    - 後者の場合、古いDatom(例えば`[63 :book/price 3672 12345 true]`)は自動的に`:db/retract`される

### Lookup Ref

- 書籍タイトルが一意ということは、タイトルが分かれば書籍エンティティを特定できるということ
- これを利用したのが、Lookup Ref

          ;; Lookup Refの例。
          [:book/title "プログラミングClojure"]
          [:bokk/title "Land of Lisp"]

  - Lookup Refをtx-dataの中やDatomic API関数の引数として使うと、エンティティIDに評価される
  - 例えば2番目は、「`:book/title`属性値として"Land of Lisp"を持つエンティティのID」という意味

## コンポーネント

- 属性エンティティの`:db/isComponent`属性にtrueを関連付けると、「全体」と「部品」の関係を定義できる

          [{:db/id #db/id [:db.part/db]
            :db/ident :book/reviews
            :db/doc "書籍のレビュー"
            :db/valueType :db.type/ref
            :db/isComponent true
            :db/cardinality :db.cardinality/many
            :db.install/_attribute :db.part/db}]

  - 書籍エンティティが全体で、レビューエンティティが部品
  - このドメインでは、レビューを書籍の一部とみなす
  - 書籍なしにレビューが存在することは無い

- 全体エンティティ作成時に、部品エンティティも作成できる

          [{:db/id #db/id [:db.part/user]
            :book/title "プログラミングClojure"
            :book/price 3672
            :book/reviews [{:review/title "オススメ"
                            :review/num-stars 5}]}
                           {:review/title "定番"
                            :review/num-stars 4}]

  - 部品エンティティの仮IDは不要
  - 実は全体・部品の関係が無くても、子エンティティ側の属性のどれかが`:db/unique`なら、上記の書き方が可能

## スキーマファイル(edn)

- スキーマ定義用のDatomは、ednファイルに記述することが多い
- それを読み込んで、トランザクションを実行する

##### customer-schema.edn

    [{:db/id #db/id [:db.part/db]
      :db/ident :customer/name
      :db/doc "カスタマーの名前"
      :db/valueType :db.type/string
      ...]

##### スキーマ用Txを実行

    (d/transact
      conn
      (-> path-to-the-customer-schema-edn
          slurp
          read-string))

- `read-string`は先頭のtx-dataだけを読む
- すべてのtx-dataを読むには、`datomic.Util`クラスの`readAll`メソッドを使う
  - [`datom-sand.util/read-edn`](../src/datom-sand/util.clj)を参照

## What's next?
- [コードを見る](../tutorial/attr-and-schema.clj)
- [3. エンティティとトランザクション](3-entity-and-tx.md)
- [5. クエリー](5-query.md)
