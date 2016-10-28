# 1. イントロ・概観

## Datomic
- Clojureの生みの親、Rich Hickeyがデザインしたデータベースシステム
- Clojureでお馴染みの「immutableデータ」をデータベースに応用
- クエリーエンジンをクライアント側へ移して負荷分散
- RDBより柔軟なスキーマシステム
- NoSQLより高いデータ表現力

## データモデル
- DBは、エンティティに関するfact(事実)の蓄積
  - factは、assertion(表明)とretraction(撤回)の2種類だけ
  - factはimmutable(蓄積した事実を書き変えることはできない)
- データの最小単位がDatom
  - 表明には、`add`用のDatomを蓄積
  - 撤回には、`retract`用のDatomを蓄積
  - Datomはimmutable


![図.データモデル](img/model.png)

- トランザクション(Tx)により、いくつかのDatom(tx-data)をatomicにDBへ蓄積する
  - Txを実行するたびに、DBのリビジョンが増えていく
  - DBを書き換えるのではなく、immutableな新しいDBスナップショットを作る(Clojureで、atomを`swap!`する感じ)
  - Gitのモデルと類似
- tx-dataを時系列に蓄積したものがDB
  - History機能がビルトイン
  - 任意の時点のDBスナップショットを取り出すことができ、それに対してクエリーを実行する

## アーキテクチャ
![図.アーキテクチャ](img/arc.png)

- ストレージ
  - ローカルディスク、RDB、クラウドなどを利用可
  - in-memoryも可能
- Transactor
  - ストレージへの書き込みを一手に担うプロセス
  - シングルスレッド
  - 蓄積オンリーのデータモデルなので、個々の書き込み処理は(RDBに比べると)軽い
- App(Peer)
  - アプリケーションのプロセス
  - Peerライブラリを使って
    - Tx実行をTransactorへ依頼
    - DBスナップショットを取得し、クエリを実行

## Datom

- Datomは、5つの要素(EAVTとoperation)からなる
  - KeyとValueしかない、NoSQLより表現力が高い
- EAVT
  - エンティティ(Entity) ...RDBの行に近いイメージ
  - 属性(Attribute) ...RDBの列に近いイメージ
  - 属性値(Value)
  - トランザクション(Tx) ...このfactがどのトランザクションで記録されたか
- operationは、`add`か`retract`のどちらか
- Datomは、これら5つの要素により、エンティティに関するfactを表現する
  - Tの時点で、Eの属性Aは、Vである ...表明
  - Tの時点で、Eの属性Aは、もはやVではない ...撤回

##### DB作成直後のDatomの例

|entity-id |attribute-id |value        |tx-id         |added|
|----------|-------------|-------------|--------------|-----|
|1         |10           |`:db/add`    |13194139533312|true |
|2         |10           |`:db/retract`|13194139533312|true |
|10        |10           |`:db/ident`  |13194139533312|true |

## [次へ](2-basics.md)
