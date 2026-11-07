# ADR 0002: C05 で htmx SSE 拡張を使う

- 状態: 採用
- 決定日: 2026-11-03
- 対応要件: F-37

## 判断

C05「サーバーイベントによるライブ通知」に限り、htmx の SSE 拡張を採用する。既存の htmx 2.0.11 と組み合わせ、固定の配布物をプロジェクト内から配信する。CDN、npm、Node.js ビルドは使わない。

| 項目 | 採用内容 |
| --- | --- |
| 拡張 | `htmx-ext-sse` 2.2.4 |
| 取得元 | [jsDelivr の固定版配布物](https://cdn.jsdelivr.net/npm/htmx-ext-sse@2.2.4) |
| ソース・利用方法 | [公式 SSE 拡張ドキュメント](https://htmx.org/extensions/sse/) / [GitHub](https://github.com/bigskysoftware/htmx-extensions/tree/main/src/sse) |
| ライセンス | BSD Zero Clause License (0BSD)。同梱した `LICENSE` を参照 |
| 配布物 SHA-256 | `98a46496de0c3605fbffdce9167ba427bdd9553184f83f149c261891a92c0136` |
| 必要性 | htmx 属性で EventSource 接続、イベント名ごとの HTML 差し替え、正常終了、再接続を学ぶため |

公式ドキュメントは htmx 2.0.11 と SSE 拡張 2.2.4 の組み合わせを示し、`sse-connect`、`sse-swap`、`sse-close` を説明している。拡張の `src/sse/LICENSE` は0BSDであることを確認し、固定版配布物とライセンス本文を同梱する。

この拡張はC05だけで有効にし、他のサンプルのグローバル依存にはしない。SSEのGET接続は読み取り専用で、入力値や業務状態を変更しない。アプリのCSPは同一オリジン接続を許可しており、外部接続は追加しない。

## 制約

- この判断はC05の学習用例に限る。WebSocket、業務用の永続通知配信、本番向けの接続基盤や認証・認可を導入する判断ではない。
- `htmx-ext-sse` の更新時は、互換性、配布元、ライセンス、チェックサムを再確認する。
- 拡張の自動再接続だけに任せず、C05側で失敗回数を制限し、停止状態と再読み込み方法を利用者へ伝える。
