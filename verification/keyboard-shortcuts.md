# Keyboard Shortcuts 検証記録

## 対象と環境

- 公式例: [htmx Keyboard Shortcuts](https://htmx.org/examples/keyboard-shortcuts/)
- 実装: `/examples/keyboard-shortcuts`
- 要件: F-01、F-04、F-06〜F-09、F-29
- 環境: Windows x64、Java 25.0.2、Spring Boot 4.1.2、htmx 2.0.11、VS Code 統合ブラウザー (Chrome 150.0.7871.250 / Electron 43.7.7)

## 自動テスト

- `.\mvnw.cmd --batch-mode --no-transfer-progress -Dtest=KeyboardShortcutsTests test` — 成功。3件、失敗0、エラー0。
- `.\mvnw.cmd --batch-mode --no-transfer-progress verify` — 成功。216件、失敗0、エラー0。
- `git diff --check` — 成功。

MockMvc では初期ページのフォームと外部 JavaScript、安全設定、htmx の断片応答、通常 GET の全ページ応答を確認しました。

## ブラウザー確認

- ボタンと Alt+Shift+D の両方で GET が1回送られ、結果パネルだけが更新されることを確認。
- 入力欄にフォーカスがある間は Alt+Shift+D で要求が発生しないことを確認。
- ボタンを Enter で送信した場合、成功後にボタンへフォーカスが戻り、ライブステータスに完了が通知されることを確認。
- 通信を遮断し、エラー通知が表示され、直前の結果パネルが保持されることを確認。
- ボタンにフォーカスした状態で通信を遮断しても、再操作できるようフォーカスが戻ることを確認。
- `/action` へ通常アクセスした場合は結果を含むページ全体が表示され、htmx の部分更新では URL が変わらないことを確認。
- 成功操作では JavaScript エラーなし。通信遮断時はブラウザーと htmx が失敗要求をコンソールに記録しますが、画面通知と表示保持は正常です。

## 未確認事項

- JavaScript を無効にした実ブラウザー操作。通常 GET フォームのページ全体応答は MockMvc と通常ナビゲーションで確認しています。
- 連続したショートカット操作・実ネットワークの遅延やタイムアウト、別ブラウザー、スクリーンリーダー、ハイコントラスト。
- OS・ブラウザー固有のショートカット競合。キーの判定はこのページ内で Alt+Shift+D に限定しています。

## 公式例との差分

公式例の `hx-trigger` 内の条件式と状態変更 POST を使わず、外部 JavaScript のキー判定と通常 GET フォームに置き換えました。画面上のボタン、入力・編集領域と開いたダイアログ内での誤発火防止、エラー通知、結果パネル保持を追加しています。追加依存はありません。
