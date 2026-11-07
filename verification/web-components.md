# Web Components 検証記録

## 対象と環境

- 対象: No.27 Web Components、要件 F-01、F-04、F-06〜F-09、F-32
- 確認日: 2026-11-01
- Java: 25.0.2
- Spring Boot: 4.1.2
- htmx: 2.0.11
- ブラウザー: VS Code 統合ブラウザー（Chromium 系。詳細バージョン未確認）

## サーバーテスト

- `.\mvnw.cmd --batch-mode -Dtest=WebComponentsTests test` — 5件成功
- `.\mvnw.cmd --batch-mode verify` — 237件成功、失敗・エラー・スキップなし
- GitHub Actions: `gh pr checks 30 --repo kazu-u/htmx_sample_project` — `test` 成功（48秒、2026-11-01確認）。

## ブラウザー確認

- `/examples/web-components` を表示し、Shadow DOM 内の「在庫の情報を取得」を押すと、在庫カードだけに「書籍 24 冊」が表示され、注文カードは未取得のまま。
- 注文カードも別に更新され、両方の内容が同じ画面に保持される。
- 在庫 URL に HTTP 404 を返す通信を模擬すると、カード内にエラーが表示され、以前の「書籍 24 冊」は残る。ページ共通のエラー表示は重複しない。
- 注文 URL の通信を中断すると、カード内に通信エラーが表示され、以前の結果が保持される。
- 通常リンクを選ぶと `/examples/web-components/summary/orders` の全ページへ移動し、結果とサンプルへ戻るリンクが表示される。
- JavaScript 実行時エラーは確認されなかった。HTTP 404 と中断の試験では、意図したエラー応答がブラウザーのコンソールに記録された。

## 未確認事項

- JavaScript 無効時のブラウザー操作、別ブラウザー、スクリーンリーダー、モバイル表示、実ネットワーク障害、実際の10秒タイムアウトは未確認。
- ブラウザーのバージョン詳細と GitHub Actions の実行結果は未確認。
