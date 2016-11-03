# Datom-sand

ClojureでDatomic。新感覚データベース**"Datomic"**の練習とチュートリアル的なもの。

## もくじ

- [1. イントロ・概観](doc/1-intro.md)
- [2. 基本](doc/2-basics.md)
- [3. エンティティとトランザクション](doc/3-entity-and-tx.md)
- [4. 属性とスキーマ](doc/4-attr-and-schema.md)
- [5. クエリー](doc/5-query.md)
- [6. Pull](doc/6-pull.md)
- [7. Rule](doc/7-rule.md)
- [8. Database function](doc/8-db-fn.md)
- [8. 履歴](doc/8-history.md)
- [9. スキーマの変更](doc/9-alter-schema.md)

## サンプルデータ

サンプルとして、書籍販売サイトのレビューのようなものを扱う。主なエンティティと属性は以下の通り。

- カスタマー
  - 名前
  - いいね
  - 都道府県
  - 紹介者
- 書籍
  - タイトル
  - 価格
  - レビュー
- レビュー
  - タイトル
  - 本文
  - 星
  - レビュー者
  - コメント
- コメント
  - 本文
  - コメント者
  - コメント

