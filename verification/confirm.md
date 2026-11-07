# Confirm 検証記録

## 対象と環境

- 対象: No.25「Confirm」、要件 F-31
- 検証日: 2026-11-01
- OS: Windows x64
- Java: 25.0.2
- Spring Boot: 4.1.2
- htmx: 2.0.11
- ブラウザー確認: VS Code 統合ブラウザー / Chrome 150.0.7871.250（Electron 43.8.3）

## 自動テスト

| コマンド | 結果 |
| --- | --- |
| `.\mvnw.cmd --batch-mode --no-transfer-progress -Dtest=ConfirmTests test` | 成功。3件、失敗0、エラー0 |
| `git diff --check` | 成功。空白エラーなし |

テストでは、確認ダイアログと通常リンク、CSRF メタ情報、htmx 要求時の結果断片と `Vary: HX-Request`、通常アクセス時の全ページを確認します。

## ブラウザー確認

アプリを `http://127.0.0.1:8080/examples/confirm` で起動して確認しました。

- 確認ボタンから標準 `<dialog>` が開き、質問が読み上げ可能な説明として表示されることを確認しました。
- 「やめる」ではダイアログが閉じ、GET 要求が送られないことを確認しました。
- Escape でもダイアログが閉じ、GET 要求が送られず、起動ボタンにフォーカスが戻ることを確認しました。
- 「続ける」では `GET /examples/confirm/result` が1回だけ送られ、結果断片が表示されることを確認しました。
- 承認後の応答完了時にも起動ボタンが有効になり、フォーカスが戻ることを確認しました。
- 503 応答をブラウザー側で模擬し、共通エラー通知が表示され、既に表示されていた結果が保たれることを確認しました。これはサーバー障害を実際に発生させた検証ではありません。
- `/examples/confirm/result` を通常アクセスし、結果を含むページ全体とサンプルへ戻るリンクが表示されることを確認しました。

## セキュリティ・状態・未確認事項

- GET は固定の結果表示だけを行い、業務状態を変更・保存しません。CSRF 対象となる更新要求はありません。
- `HX-Request` は応答形式の選択だけに使い、`Vary: HX-Request` を付けています。
- SweetAlert2 等の追加依存、インライン JavaScript、htmx の条件式を使いません。htmx の安全設定と CSP は既存設定のままです。
- **未確認:** JavaScript を無効にした実ブラウザーでの通常リンク操作。代替リンクの出力と通常 GET の全ページ応答は確認しました。
- **未確認:** 別ブラウザー、スクリーンリーダー、モバイル表示、実ネットワークの切断・遅延、実際のサーバー障害。
- GitHub Actions: PR #29 の `test` が成功しました（[実行結果](https://github.com/kazu-u/htmx_sample_project/actions/runs/41271543000)）。

## 公式例との差分

[htmx 公式 Confirm](https://htmx.org/examples/confirm/) は SweetAlert2 とインライン JavaScript を使った確認方法を示します。本サンプルは追加依存を避け、標準 `<dialog>` と外部 JavaScript の `htmx:confirm` イベントで要求の保留・再開を説明します。確認後の処理は固定 GET に限定し、JavaScript 無効時の通常リンクと共通エラー通知を加えています。
