# Datom-sand

新感覚データベース**"Datomic"**の練習とチュートリアル的なもの。

## もくじ

- [1. イントロ・概観](tree/master/doc/1-intro.md)
- [2. 基本](tree/master/doc/2-basics.md)
- [3. エンティティとトランザクション](tree/master/doc/3-entity-and-tx.md)
- [4. 属性とスキーマ](tree/master/doc/4-attr-and-schema.md)
- [5. クエリー](tree/master/doc/5-query.md)
- [6. Pull](tree/master/doc/6-pull.md)
- [7. Rule](tree/master/doc/7-rule.md)
- [8. Database function](tree/master/doc/8-db-fn.md)
- [8. 履歴](tree/master/doc/8-history.md)
- [9. スキーマの変更](tree/master/doc/9-alter-schema.md)

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

