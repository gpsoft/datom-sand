# 1. イントロ・概観

## Datomic
- Clojureの生みの親、Rich Hickeyがデザインしたデータベースシステム
- データはimmutable
- クエリーはクライアント側で実行する
- RDBより柔軟なスキーマシステム
- NoSQLより高いデータ表現力

## データモデル
- DBはfact(事実)の蓄積
  - factのassertion(表明)とretraction(撤回)
  - factはimmutable(過去の事実を書き変えることはできない)
- factを表現する最小単位がDatom
  - factのassertionは、Datomの`add`
  - factのretractionは、Datomの`retract`
  - Datomはimmutable


![図.データモデル](img/model.png)

- トランザクション(Tx)により、いくつかのDatom(tx-data)をatomicに`add`/`retract`する
  - Txを実行するたびに、DBのリビジョンが増えていく
  - Clojureのatomと`swap!`と類似
  - Gitのモデルと類似
- tx-dataを時系列に蓄積したものがDB
  - History機能がビルトイン
  - 任意の瞬間のDBスナップショットを取り出すことができ、それに対してクエリーを実行する

## アーキテクチャ
![図.アーキテクチャ](img/arc.png)

- ストレージ
  - ローカルディスク、RDB、クラウドなどを使用
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

- Datomは、あるエンティティに関するfactを宣言する
- Datomは、5つの要素(EAVTとoperation)からなる
- EAVT
  - エンティティ(Entity) ...RDBの行に近いイメージ
  - 属性(Attribute) ...RDBの列に近いイメージ
  - 値(Value)
  - トランザクション(Tx) ...このfactがどのトランザクションで記録されたか
- operationは、AddかRetractのどちらか

##### DB作成直後のDatomの例

|entity-id |attribute-id |value        |tx-id         |added|
|----------|-------------|-------------|--------------|-----|
|1         |10           |`:db/add`    |13194139533312|true |
|2         |10           |`:db/retract`|13194139533312|true |
|10        |10           |`:db/ident`  |13194139533312|true |

