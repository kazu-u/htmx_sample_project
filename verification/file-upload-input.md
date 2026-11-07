# No.14 — Preserving File Inputs after Form Errors

## 目的・要件

入力エラーの422応答でフォームを差し替えた後も、`hx-preserve` を使って選択済みのファイルを保持し、入力を修正して再送信できることを確認する。UTF-8 の `.txt` 1件、最大1 MiB、ファイル名120文字以内とし、タイトルは60文字以内。ファイル内容は検証するが表示・保存しない。

要件: F-01、F-04、F-06〜F-09、F-23。

## サーバーテスト

| コマンド | 結果 |
| --- | --- |
| `.\mvnw.cmd --batch-mode clean '-Dtest=FileUploadInputTests,FileUploadTests' '-Dsurefire.failIfNoSpecifiedTests=true' test` | 成功。No.14・No.13 の計22テスト、失敗・エラーなし |
| `.\mvnw.cmd --batch-mode clean verify` | 成功。全181テスト、失敗・エラーなし |

No.14 の11テストで、画面・`hx-preserve`・成功送信・空欄および長さ超過の422・ファイル制約・通常フォーム・CSRFを確認した。No.13 の11テストも共通バリデーター変更後に通過した。

## 実ブラウザー確認

- 環境: Windows x64、VS Code 統合ブラウザー（Chrome 150.0.7871.250）、アプリは `.\mvnw.cmd --batch-mode spring-boot:run` で `http://127.0.0.1:8080/examples/file-upload-input` に起動。アプリ実行 JDK は Java 25.0.2。
- 空タイトルと有効なUTF-8 `.txt` を送信し、422フォーム差し替え後もファイル名と選択が維持され、タイトル欄にフォーカスしてエラーが表示されることを確認。
- タイトルを入力して再送信し、成功通知、タイトル欄のクリア、ファイル選択のクリア、成功通知へのフォーカスを確認。
- `.bin` ファイルで422となり、拡張子エラー、ファイル入力の `aria-invalid` とフォーカス、選択中のファイル名が維持されることを確認。続けて `.txt` に置き換えて成功し、選択と古い `aria-invalid` が解除されることを確認。
- 横幅375pxの表示で、実効viewportと文書幅がともに360pxとなり、横方向のはみ出しがないことを確認。
- ブラウザー確認は統合ブラウザー上のPlaywrightを使い、フォームの `requestSubmit()` とファイル入力を操作した。ネイティブのマウスクリックだけによる確認はしていない。

## 未確認事項

- 1 MiB超過の413画面、JavaScript無効時の画面、通信切断時の表示は実ブラウザーでは操作していない。413、通常フォームでの422/成功リダイレクト、CSRF拒否、ファイル検証境界はMockMvcテストで確認した。
- PR #21 の GitHub Actions `test` check は実行ステップ開始前に終了し、job の steps とログは取得できなかった。実行原因は確認できず、CI のテスト結果とは扱わない。ローカル `clean verify` は181件成功。
