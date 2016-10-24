# 3. エンティティとトランザクション

## エンティティ

- エンティティはDatomの構成要素の1つ(EAVTのE)で、Datomicデータモデルの主役
- factの最小単位はDatomだが、アプリケーションレベルではエンティティを中心に考える
- エンティティに、属性値を関連付ける(または関連を切る)のがDatomの役目
  - 関連付ける ...assertion(表明)
  - 関連を切る ...retraction(撤回)
- エンティティはIDを持つ
- 1つのエンティティに関するfactを、任意数のDatomで表現する
- あるエンティティ(例えばID=32)に対し:
  - 「その*x*属性の値は*XXX*」を表明
  - 「その*y*属性の値は*YYY*」を表明
  - 「その*z*属性の値は*ZZZ*」を表明

        ;; 表明用のDatom
        [:db/add 32 x XXX]
        [:db/add 32 y YYY]
        [:db/add 32 z ZZZ]


- 一旦表明したfactを書き換えることはできないが、それを打ち消す新たなfactを追記することはできる
  - 「その*zzz*属性の値は*ZZZ*」を撤回

        ;; 撤回用のDatom
        [:db/retract 32 zzz ZZZ]      ;; もはやZZZではない


- 表明/撤回用Datomの一般形式

      [:db/add     <エンティティID> <属性ID> <属性値>]
      [:db/retract <エンティティID> <属性ID> <属性値>]

## エンティティの別名
- エンティティには`:db/ident`属性を関連付けることができる ...エンティティの別名
- IDが必要な多くの場面で、IDの代わりに別名を使うことができる

      ;; 属性IDの代わりに別名を使用
      ;; :live-in、:like、:work-forという別名を表明済みと仮定
      [:db/add 32 :live-in "Japan"]       ;; 日本に住む
      [:db/add 32 :like "Programming"]    ;; プログラミングが好きだ
      [:db/add 32 :work-for "Panasonic"]  ;; Panasonicで働いている

  - ここは少々メタ(meta-)なので、思考のストレッチが必要
  - 実は、属性もエンティティの一種
  - `:live-in`は、ある属性エンティティの別名
  - 仮にそのエンティティのIDが20だとすると、「エンティティ20の`:db/ident`属性の値は`:live-in`」を表明済み、ということ
  - `:db/ident`は、組み込み属性(これもエンティティ)の別名
- エンティティの別名にはキーワードを使う
- `:db/`や`:db.*/`で始まる別名はシステム予約

##### 別名を持つ、組み込みエンティティの例:

    :db/add
    :db/retract
    :db/ident
    :db.type/string
    :db.part/db
    :db.install/attribute
    ;; :db/idはエンティティではない(よって属性でもない。超越した存在)

## トランザクション
- 表明/撤回したいDatomのコレクションをtx-dataと呼ぶ
- トランザクションにより、tx-dataをDBへ記録する

      ;; e32は、あるエンティティのIDと仮定
      (def tx-data1
        [[:db/add e32 :live-in "Japan"]
         [:db/add e32 :like "Programming"]
         [:db/add e32 :work-for "Panasonic"]])
      (d/transact conn tx-data1)

      (def tx-data2
        [[:db/retract e32 :work-for "Panasonic"]])
      (d/transact conn tx-data2)
      ;; これ以降のDBスナップショットでは、
      ;; e32がPanasonicで働いているという事実は無い

  - `d/transact`は、トランザクションを実行する同期関数
  - 非同期版(`d/transact-async`)もある
  - どちらも、[future](http://clojure.github.io/clojure/clojure.core-api.html#clojure.core/future)を返す

- 同じエンティティIDに対する表明用Datomは、マップにまとめて表記可能

      (def tx-data3
        [{:db/id e33
          :live-in "Canada"
          :like ["Sleeping" "Eating" "Watching TV"]}
         {:db/id e34
          :live-in "France"
          :work-for "Interpol"}])
      (d/transact conn tx-data3)

  - `:like`という属性の多重度が「多(many)」と仮定している
  - その場合、属性値を2つ以上関連付けることが可能(属性の項を参照)

## トランザクション(Tx)エンティティ

- トランザクションを実行すると、トランザクション自身もエンティティとして記録される
  - エンティティなのでIDを持つ
  - デフォルトでは、`:db/txInstant`属性のみを持つ ...Tx実行時刻

- トランザクションで記録される各Datomには、TxエンティティIDが刻まれる(EAVTのT)

|entity-id |attribute-id |value        |tx-id  |added|
|----------|-------------|-------------|-------|-----|
|32        |`:live-in`   |"Japan"      |1001   |true |
|32        |`:like`      |"Programming"|1001   |true |
|32        |`:work-for`  |"Panasonic"  |1001   |true |
|32        |`:work-for`  |"Panasonic"  |1002   |false|
|33        |`:live-in`   |"Canada"     |1003   |true |
|33        |`:like`      |"Sleeping"   |1003   |true |
|33        |`:like`      |"Eating"     |1003   |true |
|33        |`:like`      |"Watching TV"|1003   |true |
|34        |`:live-in`   |"France"     |1003   |true |
|34        |`:work-for`  |"Interpl"    |1003   |true |

## 仮エンティティID

- 新規エンティティに関するDatomを表明する場合、仮エンティティIDが必要
- 仮エンティティIDを得る方法は2つ
  - `#db/id [<パーティション名> <数値>]` ...データによる方法
  - `(d/tempid <パーティション名> <数値>)` ...コードによる方法
- パーティションについては後述
- `<数値>`は、tx-data内に新規エンティティが複数ある場合の識別に使う
  - 負値以外はシステム予約
  - 同じ数値を指定すれば、同じ仮IDへ評価される
  - 数値を省略すると、`#db/id [part-name]`や`(d/tempid part-name)`は、毎回違う仮IDへ評価される

##### データ(リテラル)による仮ID取得

    #db/id [:db.part/user]
    #db/id [:db.part/user -1]
    #db/id [:db.part/user -2]

##### コードによる仮ID取得

    (d/tempid :db.part/user)
    (d/tempid :db.part/user -1)
    (d/tempid :db.part/user -2)

##### 仮IDの使用

    (def tx-data4
      [{:db/id #db/id [:db.part/user]
        :live-in "Canada"
        :like ["Sleeping" "Eating" "Watching TV"]}
       [:db/add #db/id [:db.part/user -1] :live-in "France"]
       [:db/add #db/id [:db.part/user -1] :work-for "Interpol"]])
    (d/transact conn tx-data4)    ;; 2つのエンティティを追加

## 正式なエンティティIDを得る

- `d/transact`や`d/transact-async`が返すfutureを`deref`するとマップが得られる
- マップのキーは:
  - `:db-before` ...Tx実行前のDBスナップショット
  - `:db-after` ...Tx実行後のDBスナップショット
  - `:tx-data` ...Txのtx-data
  - `:tempids` ...`d/resolve-tempid`用オブジェクト

- 正式なエンティティIDを得るには`d/resolve-tempid`を使う

##### 使用例

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

## パーティション

- すべてのエンティティは、あるパーティションの中に配置される
- 頻繁に参照しあうエンティティは、同じパーティションに配置した方がクエリーの効率が良い
- 新規エンティティの仮IDを割り当てるときに、パーティションを指定する
- パーティションも一種のエンティティである
- 組み込みパーティション

      :db.part/db            ;; 属性エンティティとパーティションエンティティを配置
      :db.part/tx            ;; Txエンティティを配置
      :db.part/user          ;; アプリ用エンティティを配置

- 独自のパーティションを作ることも可能

##### 独自のパーティションエンティティを作る

    (d/transact
      conn
      [{:db/id #db/id [:db.part/db]
        :db/ident :my-part
        :db.install/_partition :db.part/db}])