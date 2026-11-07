# C01 アクセシブルな候補選択の検証記録

## 対象

- 要件: F-33
- 画面: `/examples/accessible-combobox`
- 候補取得: `GET /examples/accessible-combobox/candidates?query=...`
- 架空担当者12件から名前・部署で検索し、最大10件を表示する。候補からの単一選択のみを受け付ける。
- 検索語は60文字以内。入力後300msで取得し、IME変換中は取得しない。JavaScriptなしでは通常GETフォームと select を使う。

## サーバーテスト

- 環境: Windows、Java 25.0.2、Spring Boot 4.1.2、Maven Wrapper 3.9.16。
- 実行: `.\mvnw.cmd --batch-mode -Dtest=AccessibleComboboxTests test`
- 実行: `.\mvnw.cmd --batch-mode -Dtest=AccessibleComboboxTests clean test`
- 結果: 成功。10 tests, 0 failures, 0 errors, 0 skipped。
- 確認範囲: 候補の部分一致・10件上限、空結果、60文字境界、422、選択IDの照合、長さ超過IDからの再選択、JavaScriptなしのフォーム用値、HTMLエスケープ、状態非保存。
- 全体検証: `.\mvnw.cmd --batch-mode clean verify` 成功。251 tests, 0 failures, 0 errors, 0 skipped。`git diff --check` も成功。
- GitHub Actions: [PR #33 の test](https://github.com/kazu-u/htmx_sample_project/actions/runs/41281823810/job/124775119246) 成功。

## ブラウザー確認

- 環境: Windows上のVS Code統合ブラウザー、Chrome 150.0.7871.250。
- JavaScript有効: 「企画」から候補1件、「佐藤」から候補1件を部分更新。矢印キーで `aria-activedescendant` が設定され、Escapeで閉じ、Enterで選択ID・表示値が更新され、確定後にサーバー結果を表示することを確認。
- JavaScript有効の通常フォーム検索: 「候補を検索」でページ全体を再表示した後、候補を自動取得して候補領域が表示され、選択可能なことを確認。
- 検索エラーからの復帰: 61文字で422とエラー表示・`aria-invalid=true` を確認後、「佐藤」に修正して候補が表示され、エラー文が消え `aria-invalid=false` となることを確認。
- 古い応答の抑止: 古い「佐藤」要求の `htmx:beforeRequest` を記録した後、「企画」の候補を表示し、古い要求の `htmx:beforeSwap` を遅れて発火。`shouldSwap=false` となり、新しい候補が維持されることをブラウザーで確認。実ネットワークの応答遅延は未確認。
- 長さ超過IDからの復帰: 41文字の候補IDによる選択確定が422になり、「候補を選び直してください」と表示される一方、検索語を保持して候補を再取得・表示できることを確認。
- IME: CompositionEventを使ったブラウザー操作で、変換中は候補要求が0件、compositionend後に1件となることを確認。実際の日本語IME入力は未確認。
- 入力エラー: maxlengthを一時的に外して61文字を入力。候補要求が422になり、入力欄の `aria-invalid=true`、エラー表示、候補リスト非表示を確認。
- JavaScript無効: CDPでスクリプト実行を無効にし、通常GETで候補を表示、`select` から選び、もう一度GETして結果が表示されることを確認。
- Node.js は実行環境に存在せず、`node --check` は実行できなかった。ただし上記ブラウザーでJavaScriptを読み込み、候補取得・選択操作を実行した。
- 未確認: 実ネットワークで遅延した候補応答の再現、HTTPエラー・タイムアウト・ネットワーク切断、別ブラウザー、スクリーンリーダー、モバイル表示。
