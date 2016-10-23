# 2. 基本
## インストール

- Datomic(Free版)をDLして解凍

      http://www.datomic.com/get-datomic.html

- 依存するJarをDL

      $ cd ~/local/datomic-free-0.9.5404
      $ bin/maven-install

- Transactorを起動(in-memoryの場合は不要)

      $ bin/transactor config/samples/free-transactor-template.properties

## プロジェクト

- プロジェクトに依存性を追加

      $ cd ~/dev
      $ lein new datom-sand
      $ cd datom-sand
      $ vim project.clj
          :dependencies [
            [com.datomic/datomic-free "0.9.5404"]
          ]
      $ lein deps
      $ lein repl

- APIのnamespaceは`datomic.api`

      $ vim src/datom-sand/core.clj
          (ns datom-sand.core
            (require [datomic.api :as d]))

- またはREPLで

      user=> (require '[datomic.api :as d])

## 準備

##### URI

    (def uri-in-memory "datomic:mem://hello")
    (def uri-local-disk "datomic:free://localhost:4334/hello")

  - Free版では、ストレージとして、in-memoryかローカルディスクを選べる。
  - `hello`がDB名
  - ローカルディスクを使う場合は、Transactorを起動しておく必要あり
  - Transactorのコンフィグによりポート番号が決まる

##### DB作成、接続

    (d/delete-database uri-in-memory)
    (d/create-database uri-in-memory)
    (def conn (d/connect uri-in-memory))


##### DBスナップショット取得

    (def db1 (d/db conn))

  - `d/db`は、現時点のDBスナップショットを返す
  - 過去や未来のDBスナップショットも取得できる
  - 別のPeerがDBを変更したとしても、`db1`には影響しない

## トランザクション(Tx)

    (d/transact
      conn
      [[:db/add                       ;; 事実の表明:
        #db/id [:db.part/user]        ;;   新規エンティティの、
        :db/doc "Hello, world!"]])    ;;   :db/doc属性の値は"Hello, world!"である。

  - トランザクションは`conn`(接続)に対して実行する ...`(d/transact conn tx-data)`
  - 第2引数(`tx-data`)には、`add`/`retract`したいDatomのコレクションを指定
  - `INSERT INTO entities(db_doc) VALUES('Hello, world!');`に近いイメージ

## クエリー

    (d/q
      '[:find ?e ?tx
        :where
        [?e :db/doc "Hello, world!" ?tx]]
      db1)

  - クエリーはDBスナップショットに対して実行する
  - `?e`や`?tx`は変数
  - `:where`に続く条件(パターン)にマッチするDatomの要素(EAVT)が変数へバインドされる
  - `SELECT eid, txid FROM entities WHERE db_doc='Hello, world!';`に近いイメージ
  - ここでは`#{}`を返す ...なぜなら`db1`はTx実行前のDBスナップショットだから


    (d/q
      '[:find ?e ?tx
        :where
        [?e :db/doc "Hello, world!" ?tx]]
      (d/db conn))

  - 最新のDBスナップショットに対してクエリーすれば、`#{[17592186045417 13194139534312]}`など、該当するDatomのエンティティIDとトランザクションIDを返す

