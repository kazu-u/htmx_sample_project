# サンプル台帳

## 台帳の見方

- 公式一覧の確認日: 2026-10-27
- 対象: [htmx Examples の UI Examples](https://htmx.org/examples/) に掲載された 28 項目
- サーバー言語別の統合リポジトリ一覧は対象外。
- 以下の説明は本プロジェクトの学習目的に合わせた日本語の要約。公式文面の逐語訳ではない。
- No.01〜16、No.19〜22、No.24〜25、No.27 の23サンプルを main にマージ済み。PR #27 は2026-10-31、PR #28〜#30 は2026-11-01に取り込みを確認。A09 も PR #31 で2026-11-01にマージされ、Actions `test` は成功。No.19〜22 の Actions `test` は GitHub Actions の利用上限に達して開始できなかった。参考扱いを除く公式例の実装を終えた。段階区分は学習内容の分類として残し、着手順には使わない。
- 追加サンプルは C01 → C02 → C03 → C04 → C05 の順で合意済み。C01 は F-33 として PR #33、C02 は F-34 として PR #34、C03 は F-35 として PR #35 で main にマージ済み。C04 は F-36 として [PR #36](https://github.com/kazu-u/htmx_sample_project/pull/36)で2026-11-03に main へマージ済み。C05 は F-37 として検証・第6回独立レビュー済みで、[PR #37](https://github.com/kazu-u/htmx_sample_project/pull/37)の Actions `test` は成功。
- 実装時には各個別ページも確認する。公式例の JavaScript・外部依存・実験的 API を、そのまま採用するとは限らない。

| 区分 | 意味 |
| --- | --- |
| 第1段階 | 基本的な取得・更新・入力検証から始める |
| 第2段階 | 遅延取得、複数領域の更新、状態・イベント連携へ広げる |
| 第3段階 | ファイル、進捗、独自 UI など個別の注意が多い例 |
| 参考 | 外部依存・認証・実験的機能等の理由で、当初は実装しない |

## 進捗管理・再開時の入口

更新日: 2026-11-03。参考扱いを除く公式例の実装と A09「ダッシュボード部品の定期更新」を完了。A09 の PR #31 は2026-11-01に main へマージされ、Actions `test` も成功。PR #30 も同日にマージ済み。No.19〜22 の Actions `test` は GitHub Actions の利用上限に達して開始できなかった。参考扱いの No.17・18・23・26・28 は対象外のため飛ばした。追加サンプル C01 は F-33 として [PR #33](https://github.com/kazu-u/htmx_sample_project/pull/33)、C02 は F-34 として [PR #34](https://github.com/kazu-u/htmx_sample_project/pull/34)、C03 は F-35 として [PR #35](https://github.com/kazu-u/htmx_sample_project/pull/35)、C04 は F-36 として [PR #36](https://github.com/kazu-u/htmx_sample_project/pull/36)で main にマージ済み。C05 は F-37 として検証・第6回独立レビュー済みで、[PR #37](https://github.com/kazu-u/htmx_sample_project/pull/37)の Actions `test` も成功。これは現在合意済みの追加サンプル列の最後だが、プロジェクト全体の最終作業を意味しない。
再開時はこの節を最初に確認し、過去の会話やコミット履歴から次の対象を推測しない。

### 状態と更新ルール

| 状態 | 判定条件 |
| --- | --- |
| 未着手 | 対象は決まっているが、実装は開始していない |
| 実装中 | 実装・文書更新・検証のいずれかが進行中 |
| 検証済み | サンプルの完了条件を満たし、対象テストと実ブラウザー確認の結果・未確認事項を記録した |
| レビュー済み | 最新差分の独立レビューと指摘の対応を完了した。以後の変更は関連検証と再レビューが必要 |
| マージ済み | 人の承認を経て main に取り込まれた |

- 新しい作業は対象範囲・要件 ID を決めてから「現在のタスク」に記録する。未決定の対象を勝手に着手済みにしない。
- 完了チェックは証跡を確認してから付ける。未実行・未確認・対象外はチェックせず、理由を記録する。
- 実装の完了条件は [実装・解説ガイド](implementation-guide.md#6-一つのサンプルの完了条件)、独立レビューは [レビュー方針](review-policy.md) を正とする。
- 詳細な検証結果は検証記録、レビュー指摘と判断は PR に置き、この節にはリンクと再開に必要な情報だけを残す。
- 中断時・検証完了時・レビュー完了時にこの節を更新する。マージ後は次の作業開始時に取り込みを確認し、状態と PR を更新する。
- A01〜A08 は独立画面としての採用済みタスクではなく、各サンプルの共通品質として扱う。A09 は公式例の実装後に行う採用済みタスクとして仕様合意・実装・マージを完了した。

### 直近の完了タスク

- 対象: A09 ダッシュボード部品の定期更新。
- 状態: マージ済み。
- 目的・対象範囲: 架空の注文・在庫カードを個別に5秒ごとに GET し、表示中の累計5分を上限に自動更新する。一時停止・再開、非表示タブでの停止と復帰時の再取得、カード別の手動更新、前回値を保つエラー表示と手動復帰を扱う。値は時刻から算出し、GETで状態を変更しない。
- 要件 ID: F-01、F-04、F-06〜F-10。
- 作業ブランチ: `feature/dashboard-polling`。
- 関連 PR: [#31](https://github.com/kazu-u/htmx_sample_project/pull/31)（2026-11-01 main へマージ、Actions `test` 成功）。
- 検証記録: [検証記録](../verification/dashboard-polling.md)。
- 当時の次の操作: 追加サンプルの優先順位は C01 → C02 → C03 → C04 → C05 で合意済み。C01の具体仕様合意と実装は次のタスクとした。
- 未解決事項: Node.js による個別構文検査、JavaScript 無効での実操作、実際のタイムアウト、ネットワーク切断、別ブラウザー、スクリーンリーダー、モバイル表示は未確認。JavaScript は実ブラウザーで読み込み・実行を確認した。
- [x] 次の対象と大枠の目的を決定した
- [x] 対象範囲・制約を決定した
- [x] 実装・異常系対応を完了した
- [x] 関連文書・画面解説・台帳を更新した
- [x] 対象サーバーテストを実行し、コマンドと結果を記録した
- [x] 実ブラウザーで確認し、環境・結果・未確認事項を記録した
- [x] 最新差分の独立レビューと指摘の対応を完了した
- [x] 人の承認によるマージと main への取り込みを確認した
- 作業ツリーの注意: ローカルに未追跡の `.classpath`、`.project`、`.settings/` がある。今回の対象外で、追加・削除しない。

### 直近の完了タスク（C01）

- 対象: C01 アクセシブルな候補選択（Autocomplete / Combobox）。
- 状態: マージ済み。
- 目的・対象範囲: 架空の担当者12件を名前・部署で検索し、入力後300msで最大10件を取得する。候補選択のみを許可し、IME・キーボード・ARIA・古い応答の抑止、サーバー側ID検証、JavaScriptなしの通常GETフォームを扱う。
- 合意済み仕様: 利用者が架空の社内担当者（氏名・部署）、候補のみ（自由入力なし）、300ms待機、最大10件、JavaScriptなしの検索・選択代替を指定。IME変換中は要求せず、候補は矢印キーとEnterで選び、Escapeで閉じる。
- 要件 ID: F-33。
- 作業ブランチ: `feature/accessible-combobox`。
- 関連 PR: [#33](https://github.com/kazu-u/htmx_sample_project/pull/33)。
- 検証記録: [C01 検証記録](../verification/accessible-combobox.md)。
- 次の操作: 優先順位に従い、C02 の具体仕様・要件・検証範囲を合意してから着手する。
- 未解決事項: 実ネットワークでの遅延応答、実際の日本語IME、別ブラウザー・スクリーンリーダー・タイムアウト・ネットワーク切断は未確認。対象サーバーテスト10件とChromeでの候補操作・JavaScript無効操作を確認済み。通常フォーム検索後の候補表示、422後の復帰、長さ超過IDからの再選択、古い応答差し替え拒否のブラウザー確認も記録済み。Node.jsは環境になく個別構文検査は未実施。
- [x] 対象範囲と仕様を合意した
- [x] 実装・異常系対応を行い、対象サーバーテストを実行した
- [x] 関連文書・台帳を更新した
- [x] 実ブラウザーで確認し、環境・結果・未確認事項を記録した
- [x] 人の承認によるマージと main への取り込みを確認した
- 独立レビュー結果: Independent Reviewer が読み取り・検索のみで10回実施。第8回で全機能差分、第10回でPR・CI状態の差分を確認し、最終指摘なし。中間指摘は修正済みで、結果をPR本文に記録。

### 完了タスク（C02）

- 対象: URL 駆動の絞り込み・並べ替え・ページング表。
- 状態: マージ済み。
- 合意済み仕様: 架空の配送注文30件を注文番号・顧客名で検索し、配送状態で絞り込む。注文番号・顧客名・注文日で並べ替え、1ページ5件。キーワード・状態・ソート列・方向・ページをGETクエリに含め、直接アクセス・再読み込み・共有URL・戻る/進むで状態を再現する。JavaScriptなしでは通常GETフォーム・リンクを使い、DB・状態保存・追加依存は行わない。
- 要件 ID: F-34。
- 作業ブランチ: `feature/url-driven-table`。
- 関連 PR: [#34](https://github.com/kazu-u/htmx_sample_project/pull/34)（2026-11-03 main へマージ、Actions `test` 成功）。
- 検証記録: [C02 検証記録](../verification/url-driven-table.md)。
- 次の操作: C02 は PR #34 で main にマージ済み。現在の進捗は下記の現在のタスクを参照。
- 未解決事項: JavaScript無効の実ブラウザー操作、実ネットワークの遅延・切断・タイムアウト、別ブラウザー、スクリーンリーダー、モバイル幅は未確認。統合ブラウザーで別コンテキストを作成できず、JavaScript無効の試験は未実施。
- [x] 対象範囲と仕様を合意した
- [x] 実装・異常系対応を行い、対象サーバーテストを実行した
- [x] 関連文書・画面解説・台帳を更新した
- [x] 実ブラウザーで確認し、環境・結果・未確認事項を記録した
- [x] 最新差分の独立レビューと指摘の対応を完了した
- [x] 人の承認によるマージと main への取り込みを確認した

### 直近の完了タスク（C03）

- 対象: 複数ステップフォーム。
- 状態: マージ済み。
- 合意済み仕様: 架空の出張申請を「申請者」「出張内容」「確認・完了」の3段階で入力する。段階別のサーバー検証、画面内の戻る・次へ、入力保持、セッション単位の下書き、完了・リセット、CSRF、古い画面の409、htmx断片と通常フォームの両立を扱う。完了はセッション内表示のみで外部保存・業務登録はしない。
- 要件 ID: F-35。
- 作業ブランチ: `feature/multi-step-form`。
- 関連 PR: [#35](https://github.com/kazu-u/htmx_sample_project/pull/35)（2026-11-03 main へマージ）。
- 検証記録: [C03 検証記録](../verification/multi-step-form.md)。ローカル全269テストと PR の Actions `test` が成功。
- 次の操作: 合意済みの C04 / F-36 の独立レビューを実施し、PRを作成する。
- 未解決事項: JavaScript無効の実ブラウザー操作、実際の20分経過によるセッション失効、ネットワーク切断・タイムアウト、別ブラウザー、スクリーンリーダー、モバイル幅は未確認。通常フォーム経路はMockMvcで確認済み。
- [x] 対象範囲と仕様を合意した
- [x] 実装・異常系対応を行い、対象サーバーテストを実行した
- [x] 関連文書・画面解説・台帳を更新した
- [x] 実ブラウザーで確認し、環境・結果・未確認事項を記録した
- [x] 最新差分の独立レビューと指摘の対応を完了した
- [x] 人の承認によるマージと main への取り込みを確認した

### 直近の完了タスク（C04）

- 対象: 取り消せる削除・操作。
- 状態: マージ済み。
- 合意済み仕様: セッションごとに分離した固定の架空タスク5件で、削除POST後30秒間は削除待ちとし、取り消しPOSTで復元する。期限後は取り消し不可・表示上削除済みとし、GETは状態を書き換えない。件数固定のため期限切れ記録は一覧に保持し、明示リセットまたは20分のセッション失効で回収する。バックグラウンドジョブ・DB・外部API・追加依存は使わない。CSRF、通常フォーム代替、状態版、409競合、404、セッション分離を扱う。
- 要件 ID: F-36。
- レビュー指摘・対応: 第1回 P1を受けて一覧revisionを追加。第2回の指摘を受け、期限由来の状態を含む一覧tokenを行一覧と同じ同期スナップショットから生成するよう修正し、期限境界・古い初期化の回帰テストを追加。第3回の修正後レビューは指摘なし。
- 作業ブランチ: `feature/undoable-delete`。
- 検証記録: [C04 検証記録](../verification/undoable-delete.md)。
- 関連 PR: [#36](https://github.com/kazu-u/htmx_sample_project/pull/36)（2026-11-03 main へマージ）。
- 未解決事項: JavaScript無効の実ブラウザー操作、ネットワーク障害、別ブラウザー、スクリーンリーダー、モバイル幅は未確認。通常フォーム経路はMockMvcで確認済み。
- [x] 対象範囲と仕様を合意した
- [x] 実装・異常系対応を完了した
- [x] 関連文書・画面解説・台帳を更新した
- [x] 対象サーバーテストを実行し、コマンドと結果を記録した
- [x] 実ブラウザーで確認し、環境・結果・未確認事項を記録した
- [x] 最新差分の独立レビューと指摘の対応を完了した
- [x] 人の承認によるマージと main への取り込みを確認した

### 現在のタスク（C05）

- 対象: サーバーイベントによるライブ通知（SSE）。
- 状態: 検証済み。第6回独立レビュー済み（指摘なし）。PR #37 作成済み、Actions `test` 成功。
- 目的・対象範囲: 架空の通知5件を1つのSSE接続で約3秒ごとにHTML断片として配信し、一覧へ追加する。イベントIDと `Last-Event-ID` で切断後の続きから再開し、完了イベントで接続を閉じる。同時接続8件・接続20秒の上限、予約タスクの後始末、連続5回の失敗停止・通知を扱う。JavaScript無効時は通常ページと再読み込みを案内し、DB・外部API・業務状態は使わない。htmx SSE 拡張 2.2.4 を0BSDライセンスとともにローカル配信する。
- 要件 ID: F-37。
- 作業ブランチ: `feature/server-sent-events`。
- 関連 PR: [PR #37](https://github.com/kazu-u/htmx_sample_project/pull/37)。
- 拡張配布・ライセンス: [公式 SSE 拡張](https://htmx.org/extensions/sse/)、[配布元](https://github.com/bigskysoftware/htmx-extensions/tree/main/src/sse)、[ADR 0002](adr/0002-htmx-sse-extension.md)。
- 検証記録: [C05 検証記録](../verification/server-sent-events.md)。
- 次の操作: PRレビュー対応後に人の承認を得てマージする。
- 未解決事項: JavaScript無効時の表示は実ブラウザー未確認。その他の未確認事項は検証記録を参照。
- [x] 対象範囲と仕様を合意した
- [x] 実装・異常系対応を行い、対象サーバーテストを実行した
- [x] 関連文書・画面解説・台帳を更新した
- [x] 実ブラウザーで確認し、環境・結果・未確認事項を記録した
- [ ] 最新差分の独立レビューと指摘の対応を完了した
- [ ] PR 作成を完了した

着手時は、以下の様式でこの節を置き換える。

```markdown
- 対象: No.XX / サンプル名
- 状態: 未着手
- 目的・対象範囲:
- 要件 ID:
- 作業ブランチ:
- 関連 PR: 未作成
- 検証記録:

完了チェック:
- [ ] 対象範囲を決定し、実装・異常系対応を完了した
- [ ] 関連文書・画面解説・台帳を更新した
- [ ] 対象サーバーテストを実行し、コマンドと結果を記録した
- [ ] 実ブラウザーで確認し、環境・結果・未確認事項を記録した
- [ ] 最新差分の独立レビューと指摘の対応を完了した
- [ ] 人の承認によるマージと main への取り込みを確認した

再開メモ:
- 次の操作:
- 未解決事項・未実行・対象外の理由:
```

### サンプル別の進捗

下表は各公式例の進捗を示す。当初の実装対象はすべてマージ済みで、参考扱いの項目は対象外、A09 もマージ済み。追加サンプル C01〜C04 は PR #33〜#36 でマージ済み。C05 は F-37 として検証・第6回独立レビュー済みで、PR #37 の Actions `test` は成功。
過去の個別チェックを今回の作業で再検証したという意味ではない。検証の範囲・未確認事項は各検証記録を参照し、過去のレビュー完了を推測でチェックし直さない。
参考扱いの No.17、18、23、26、28 は従来どおり当初の対象外とし、この実装順には含めない。採用する場合は依存・認証・実験的機能等の方針を別途確認する。

| No. | サンプル | 状態 | 証跡・関連 PR |
| --- | --- | --- | --- |
| 01 | Click To Edit | マージ済み | [検証記録](../verification/click-to-edit.md) |
| 02 | Bulk Update | マージ済み | [検証記録](../verification/bulk-update.md) / [#14](https://github.com/kazu-u/htmx_sample_project/pull/14)。独立レビュー3回、2026-10-31 main 取り込み確認 |
| 03 | Click To Load | マージ済み | [検証記録](../verification/click-to-load.md) |
| 04 | Delete Row | マージ済み | [検証記録](../verification/delete-row.md) |
| 05 | Edit Row | マージ済み | [検証記録](../verification/edit-row.md) |
| 06 | Lazy Loading | マージ済み | [検証記録](../verification/lazy-loading.md) / [#15](https://github.com/kazu-u/htmx_sample_project/pull/15)。2026-10-31 main取り込み確認 |
| 07 | Inline Validation | マージ済み | [検証記録](../verification/inline-validation.md) |
| 08 | Infinite Scroll | マージ済み | [検証記録](../verification/infinite-scroll.md) / [#16](https://github.com/kazu-u/htmx_sample_project/pull/16)。2026-10-31 main取り込み確認 |
| 09 | Active Search | マージ済み | [検証記録](../verification/active-search.md) / [#13](https://github.com/kazu-u/htmx_sample_project/pull/13) |
| 10 | Progress Bar | マージ済み | [検証記録](../verification/progress-bar.md) / [#17](https://github.com/kazu-u/htmx_sample_project/pull/17)。2026-10-31 main取り込み確認 |
| 11 | Value Select | マージ済み | [検証記録](../verification/value-select.md) / [#18](https://github.com/kazu-u/htmx_sample_project/pull/18)。2026-10-31 main取り込み確認。CI未実行（課金・利用上限） |
| 12 | Animations | マージ済み | [検証記録](../verification/animations.md) / [#19](https://github.com/kazu-u/htmx_sample_project/pull/19)。2026-10-31 main 取り込み確認。CI は課金・利用上限により開始されず |
| 13 | File Upload | マージ済み | 要件 F-22。[検証記録](../verification/file-upload.md) / [#20](https://github.com/kazu-u/htmx_sample_project/pull/20)。2026-10-31 main取り込み確認。Actions は課金・利用上限で runner 起動前に終了 |
| 14 | Preserving File Inputs after Form Errors | マージ済み | 要件 F-23。[検証記録](../verification/file-upload-input.md) / [#21](https://github.com/kazu-u/htmx_sample_project/pull/21)。2026-10-31 main取り込み確認 |
| 15 | Reset User Input | マージ済み | 要件 F-24。[検証記録](../verification/reset-user-input.md) / [#22](https://github.com/kazu-u/htmx_sample_project/pull/22)。2026-10-31 main取り込み確認 |
| 16 | Dialogs - Browser | マージ済み | 要件 F-25。公式掲載順。[検証記録](../verification/dialogs-browser.md) / [#23](https://github.com/kazu-u/htmx_sample_project/pull/23)。2026-10-31 main取り込み確認。Actions はテスト開始前に失敗、ログなし |
| 19 | Dialogs - Custom | マージ済み | 要件 F-26。公式掲載順。[検証記録](../verification/dialogs-custom.md) / [#24](https://github.com/kazu-u/htmx_sample_project/pull/24)。2026-10-31 main取り込み確認。Actions は2回ともテスト開始前に失敗、ログなし |
| 20 | Tabs (Using HATEOAS) | マージ済み | 要件 F-27。公式掲載順。[検証記録](../verification/tabs-hateoas.md) / [#25](https://github.com/kazu-u/htmx_sample_project/pull/25)。独立レビュー2回、最終差分12ファイル・指摘なし。Actions の test は失敗扱い、ログなし |
| 21 | Tabs (Using JavaScript) | マージ済み | 要件 F-28。公式掲載順。[検証記録](../verification/tabs-javascript.md) / [#26](https://github.com/kazu-u/htmx_sample_project/pull/26)。2026-10-31 main取り込み確認。Actions `test` は実行ステップ前に失敗、ログなし |
| 22 | Keyboard Shortcuts | マージ済み | 要件 F-29。公式掲載順。[検証記録](../verification/keyboard-shortcuts.md) / [#27](https://github.com/kazu-u/htmx_sample_project/pull/27)。2026-10-31 main 取り込み確認。Actions `test` は開始前に失敗、ログなし |
| 24 | Updating Other Content | マージ済み | 要件 F-30。[検証記録](../verification/updating-other-content.md) / [#28](https://github.com/kazu-u/htmx_sample_project/pull/28)。2026-11-01 main 取り込み確認 |
| 25 | Confirm | マージ済み | 要件 F-31。[検証記録](../verification/confirm.md) / [#29](https://github.com/kazu-u/htmx_sample_project/pull/29)。Actions `test` 成功、2026-11-01 main 取り込み確認 |
| 27 | Web Components | マージ済み | 要件 F-32。[検証記録](../verification/web-components.md) / [#30](https://github.com/kazu-u/htmx_sample_project/pull/30)。2026-11-01 main 取り込み、Actions `test` 成功 |
| A09 | ダッシュボード部品の定期更新 | マージ済み | 要件 F-10。[検証記録](../verification/dashboard-polling.md) / [#31](https://github.com/kazu-u/htmx_sample_project/pull/31)。2026-11-01 main 取り込み確認 |

### No.24 の実装状況

- 実装先: `/examples/updating-other-content`。セッション単位の架空連絡先を追加し、最大20件で制限する。
- 主な更新: `#contacts-panel` を差し替え、件数と通知を `hx-swap-oob` で更新する。更新範囲を広げる案とイベント発火案も画面で比較する。
- 競合対策: 各成功更新で版を進め、同一セッションの古いフォームからの更新は409で拒否する。遅延応答も画面側で古い版なら差し替えない。
- 検証: [検証記録](../verification/updating-other-content.md)を参照。

### No.25 の実装範囲

- 実装先: `/examples/confirm`。標準 `<dialog>` と `htmx:confirm` イベントで要求前に確認する。
- 承認時は固定の GET で結果を取得し、キャンセル・Escape では送信しない。サーバー状態は変更しない。
- JavaScript 無効時は同じ結果を通常ページで表示するリンクを用意する。SweetAlert2 等の追加依存・インライン JavaScript は使わない。
- 検証記録: [検証記録](../verification/confirm.md)を参照。

### No.27 の実装範囲

- 実装先: `/examples/web-components`。カスタム要素の Shadow DOM 内に置いたボタンを `htmx.process` で初期化し、各カードの内部だけを更新する。
- 固定データの GET とし、エラー時は直前の結果を残してカード内に通知する。通常リンクから同じ内容を全ページで表示できる。
- htmx の安全設定・CSPを維持し、テンプレート複製と `textContent` を使う。インラインスクリプト、追加依存、状態変更は行わない。
- 検証記録: [検証記録](../verification/web-components.md)。

## 公式 UI Examples

| No. | 公式例・参照先 | 日本語での学習内容 | 区分 | 実装時の注意・学習ポイント |
| --- | --- | --- | --- | --- |
| 01 | [Click To Edit](https://htmx.org/examples/click-to-edit/) | 表示内容をクリックして、その場で編集・保存する | 第1段階・最初 | 表示と編集フォームの差し替え、保存、キャンセル、入力エラー |
| 02 | [Bulk Update](https://htmx.org/examples/bulk-update/) | 全行の有効・無効をチェック状態どおり一括保存する | 第2段階 | 全解除、不正 ID、処理対象の検証、一括処理の整合性 |
| 03 | [Click To Load](https://htmx.org/examples/click-to-load/) | 「もっと見る」で一覧の続きを追加する | 第1段階 | 行の追記、末尾到達、重複取得の防止 |
| 04 | [Delete Row](https://htmx.org/examples/delete-row/) | 一覧の行を削除し、その行だけ画面から除く | 第1段階 | 更新メソッド、CSRF、削除確認、失敗時に行を残す |
| 05 | [Edit Row](https://htmx.org/examples/edit-row/) | 一覧の一行を編集用フォームに切り替える | 第1段階 | 行単位の更新先、保存、キャンセル、フォーカス |
| 06 | [Lazy Loading](https://htmx.org/examples/lazy-load/) | 初期画面を先に出し、必要な内容を後から取得する | 第2段階 | ローディング表示、取得失敗、再試行 |
| 07 | [Inline Validation](https://htmx.org/examples/inline-validation/) | 入力欄の近くに検証結果を表示する | 第1段階 | 検証タイミング、サーバー検証、保存時の再検証 |
| 08 | [Infinite Scroll](https://htmx.org/examples/infinite-scroll/) | スクロール位置に応じて続きを読み込む | 第2段階 | 終端、二重取得、手動の読み込み導線 |
| 09 | [Active Search](https://htmx.org/examples/active-search/) | 入力に応じて検索結果を更新する | 第1段階 | 待ち時間、日本語 IME、古い応答の反映防止、0 件 |
| 10 | [Progress Bar](https://htmx.org/examples/progress-bar/) | サーバーの処理状況を定期取得して進捗を表示する | 第3段階 | ポーリング、開始と参照の分離、失敗・キャンセル・終了後の停止 |
| 11 | [Value Select](https://htmx.org/examples/value-select/) | 親の選択に応じて子の選択肢を変える | 第1段階 | 選択解除、子の値のリセット、不正な親子関係の検証 |
| 12 | [Animations](https://htmx.org/examples/animations/) | 差し替え時に CSS の動きを付ける | 第2段階 | swap / settle、動きを減らす設定、操作の妨げを避ける |
| 13 | [File Upload](https://htmx.org/examples/file-upload/) | ファイルを送信し、通信の進捗を表示する | 第3段階 | multipart、容量上限、一時データ削除。送信進捗とサーバー処理進捗は別 |
| 14 | [Preserving File Inputs after Form Errors](https://htmx.org/examples/file-upload-input/) | フォームの検証エラーで、選択済みファイルが消えるのを避ける | 第3段階 | ファイル入力の保持。ページ再読み込み後まで復元できるとは限らない |
| 15 | [Reset User Input](https://htmx.org/examples/reset-user-input/) | 送信に成功したときだけ入力欄を初期化する | 第2段階 | エラー時の入力保持、成功判定、標準 JavaScript のイベント処理 |
| 16 | [Dialogs - Browser](https://htmx.org/examples/dialogs/) | ブラウザー標準の確認・入力ダイアログを使う | 第1段階 | `hx-confirm` / `hx-prompt`、キャンセル、入力値の検証 |
| 17 | [Dialogs - UIKit](https://htmx.org/examples/modal-uikit/) | UIkit のモーダルと部分取得を組み合わせる | 参考 | UIkit が必要。独自ダイアログとの差を説明し、初期依存には加えない |
| 18 | [Dialogs - Bootstrap](https://htmx.org/examples/modal-bootstrap/) | Bootstrap のモーダルと部分取得を組み合わせる | 参考 | Bootstrap が必要。初期依存には加えない |
| 19 | [Dialogs - Custom](https://htmx.org/examples/modal-custom/) | 外部 UI ライブラリなしでダイアログを構成する | 第3段階 | 標準 `dialog` 要素等で構成。Esc、フォーカス移動・復帰、閉じる操作 |
| 20 | [Tabs (Using HATEOAS)](https://htmx.org/examples/tabs-hateoas/) | サーバーが選択状態を含む HTML を返し、タブを切り替える | 第2段階 | 状態をどこが管理するか、選択状態とキーボード操作 |
| 21 | [Tabs (Using JavaScript)](https://htmx.org/examples/tabs-javascript/) | JavaScript 側でも選択状態を扱い、タブを切り替える | 第2段階 | No.20 との責務比較。外部 JS、二重の状態管理を避ける |
| 22 | [Keyboard Shortcuts](https://htmx.org/examples/keyboard-shortcuts/) | キーボードから画面の操作を実行する | 第2段階 | 入力中の誤発火防止、代替ボタン、条件式は安全設定に合わせて代替 |
| 23 | [Drag & Drop / Sortable](https://htmx.org/examples/sortable/) | ドラッグで行を並べ替え、順序を送信する | 参考 | 公式例は Sortable.js を使用。標準機能での代替は別案として扱う |
| 24 | [Updating Other Content](https://htmx.org/examples/update-other-content/) | 主な更新先以外の件数や通知なども更新する | 第2段階 | OOB 更新・イベント等の比較、一貫した状態表示 |
| 25 | [Confirm](https://htmx.org/examples/confirm/) | 更新前に独自の確認手順を差し込む | 第3段階 | 標準確認との違い、イベント連携、重複送信とキャンセル |
| 26 | [Async Authentication](https://htmx.org/examples/async-auth/) | 非同期で認証情報を準備してからリクエストを送る | 参考 | 当初は認証なし。実装する場合は認証方式とトークンの扱いを先に決める |
| 27 | [Web Components](https://htmx.org/examples/web-components/) | Web Components / Shadow DOM と htmx を連携させる | 第3段階 | ブラウザー標準機能を使う。DOM 境界、初期化、イベントの到達範囲 |
| 28 | [(Experimental) moveBefore()-powered hx-preserve](https://htmx.org/examples/move-before/) | 実験的な DOM 移動 API と要素保持の関係を確認する | 参考 | ブラウザー対応、API の安定性、未対応時の挙動。基本機能の前提にしない |

### No.02 の実装状況

- マージ済み: `/examples/bulk-update`。要件 F-01、F-04〜F-09、F-16。独立レビュー3回、最終指摘なし。[#14](https://github.com/kazu-u/htmx_sample_project/pull/14) の main 取り込みを2026-10-31に確認。公式ページ再確認: 2026-10-31。
- [Controller](../src/main/java/com/example/htmx/examples/bulkupdate/BulkUpdateController.java)、[フォーム](../src/main/java/com/example/htmx/examples/bulkupdate/BulkUpdateForm.java)、[状態](../src/main/java/com/example/htmx/examples/bulkupdate/BulkUpdateState.java)、[画面](../src/main/resources/templates/examples/bulk-update.html)、[テスト](../src/test/java/com/example/htmx/BulkUpdateTests.java)
- 利用者の選択により公式例と同じ全行保存方式を採用。架空の4人をセッション単位で保持し、チェックなしは無効、全解除も有効な保存操作とする。
- ID・件数・重複・版を検証して、不正入力は422で全件拒否。古い版は409、保存と版の照合は同じ排他区間。通常フォーム代替と版照合付き初期化を用意する。
- 公式例の通知だけの差し替えとは異なり、保存済み状態と新しい版情報の整合のためフォーム領域を置換する。CSRF、入力保持、フォーカス、未保存の変更通知、日本語解説を追加。追加依存なし。
- 検証環境・結果・未確認事項は [検証記録](../verification/bulk-update.md) を参照。

### No.08 の実装状況

- 実装・検証済み: `/examples/infinite-scroll`。要件 F-01、F-04、F-06〜F-09、F-18。公式ページ再確認: 2026-10-31。レビュー・PR状況は「現在のタスク」を参照。
- [Controller](../src/main/java/com/example/htmx/examples/infinitescroll/InfiniteScrollController.java)、[画面](../src/main/resources/templates/examples/infinite-scroll.html)、[行断片](../src/main/resources/templates/examples/infinite-scroll-rows.html)、[テスト](../src/test/java/com/example/htmx/InfiniteScrollTests.java)、[検証記録](../verification/infinite-scroll.md)。
- 架空の固定9件を3件ずつ取得し、3ページで終了。枠内のintersect onceと手動submitを同じtbodyで同期。操作行を置換して既存行を保持し、最後はトリガーのない終了行を返す。
- 失敗時は既存行・ボタンを残し、明示的に再試行する。通常フォームは指定ページまで累積表示する。手動時のフォーカス復帰・処理中・通知を追加。追加依存・業務状態・DB・外部APIなし。

### No.10 の実装状況

- 実装・検証・独立レビュー済み: `/examples/progress-bar`。要件 F-01、F-04、F-06〜F-09、F-19。第1回・第2回レビュー指摘を修正、第3回レビューで指摘なし。PR状況は「現在のタスク」を参照。
- [Controller](../src/main/java/com/example/htmx/examples/progressbar/ProgressBarController.java)、[セッション状態](../src/main/java/com/example/htmx/examples/progressbar/ProgressJobState.java)、[画面](../src/main/resources/templates/examples/progress-bar.html)、[進捗断片](../src/main/resources/templates/examples/progress-state.html)、[操作断片](../src/main/resources/templates/examples/progress-panel.html)、[テスト](../src/test/java/com/example/htmx/ProgressBarTests.java)、[検証記録](../verification/progress-bar.md)。
- 開始・キャンセルはCSRF保護されたPOST、進捗は副作用のないGET。開始時に結果（成功・失敗）を選び、開始時刻から10秒で進捗を計算する。セッションごとの状態は1件だけで、実ワーカー・DB・外部APIは使わない。
- ポーリング対象を進捗表示だけにして操作部のフォーカスを保つ。終端時はOOBで操作を再開可能にし、定期取得を終了する。通信失敗時は表示を保持して自動取得を止め、手動再試行を案内する。
- 追加依存なし。環境・テスト・ブラウザー結果・未確認事項は検証記録を参照。

### No.11 の実装状況

- マージ済み: `/examples/value-select`。要件 F-01、F-04、F-06〜F-09、F-20。PR #18 は2026-10-31に main へ取り込み確認。GitHub Actions は課金・利用上限のため未実行。
- 固定の架空データ3組を使い、親選択で子候補を取得する。選択はサーバーに保存せず、最終確認で親子関係を再検証する。
- 仕様の選択: 初期は親子とも未選択。親変更・解除で子の選択をリセット。親と子を明示送信して結果を表示。組み合わせ不正時は422で親を保持し、子を再選択させる。JavaScript無効時は候補表示ボタンと通常GETフォームを使う。
- [Controller](../src/main/java/com/example/htmx/examples/valueselect/ValueSelectController.java)、[画面](../src/main/resources/templates/examples/value-select.html)、[テスト](../src/test/java/com/example/htmx/ValueSelectTests.java)。
- [検証記録](../verification/value-select.md)。独立レビューのP2指摘を修正。PR #18でマージ済み。

### No.12 の実装状況

- マージ済み: `/examples/animations`。要件 F-01、F-04、F-06〜F-09、F-21。PR #19 を2026-10-31に main へ取り込み確認。GitHub Actions は支払い失敗または利用上限のためジョブが開始されず。
- 固定表示のカードを htmx GET で切り替え、同じ ID を維持する。`htmx-added` クラスが settle 時に外れることで新しいカードを opacity transition でフェードインし、色・背景色は新しい要素にすぐ適用する。
- `prefers-reduced-motion`、要求中の操作抑止・通知、通常 GET フォームとエラー通知を扱う。View Transitions API、htmx 拡張、追加依存は使わない。
- [画面](../src/main/resources/templates/examples/animations.html)、[CSS](../src/main/resources/static/css/animations.css)、[Controller](../src/main/java/com/example/htmx/examples/animations/AnimationsController.java)、[テスト](../src/test/java/com/example/htmx/AnimationsTests.java)、[検証記録](../verification/animations.md)。独立レビュー3回、P2指摘2件を修正し、最終レビューに指摘なし。

### No.13 の実装状況

- マージ済み: `/examples/file-upload`。PR #20 は2026-10-31に main へ取り込み済み。GitHub Actions は支払い失敗または利用上限により runner 起動前に終了したが、ローカル `clean verify` は170件成功。要件 F-01、F-04、F-06〜F-09、F-22。
- 受け入れるのは UTF-8 の `.txt` 1件、最大1 MiB、ファイル名120文字まで。拡張子と内容を検証し、MIME 型の申告だけに依存しない。内容は表示・保存せず、要求終了後に破棄する。
- 対象 XHR の `upload` に登録した標準 `progress` イベントで通信転送進捗を表示し、送信と応答受信の進捗を混同しない。公式例の `htmx:xhr:progress` とは購読方法を変え、サーバー処理の進捗とも区別する。Hyperscript・追加依存は使わない。
- [Controller](../src/main/java/com/example/htmx/examples/fileupload/FileUploadController.java)、[画面](../src/main/resources/templates/examples/file-upload.html)、[CSS](../src/main/resources/static/css/file-upload.css)、[JavaScript](../src/main/resources/static/js/file-upload.js)、[テスト](../src/test/java/com/example/htmx/FileUploadTests.java)、[検証記録](../verification/file-upload.md)。11件の専用テストと実Tomcatでフォーム本文のCSRF・容量・一時ファイル後始末を確認。ブラウザーで送信進捗、成功、422、413、狭幅を確認。

### No.15 の実装状況

- マージ済み: `/examples/reset-user-input`。要件 F-01、F-04、F-06〜F-09、F-24。公式例を2026-10-31に確認。[#22](https://github.com/kazu-u/htmx_sample_project/pull/22) の main 取り込みを2026-10-31に確認。独立レビュー3回、最終差分に指摘なし。
- [Controller](../src/main/java/com/example/htmx/examples/resetuserinput/ResetUserInputController.java)、[入力フォーム](../src/main/java/com/example/htmx/examples/resetuserinput/NoteForm.java)、[画面](../src/main/resources/templates/examples/reset-user-input.html)、[応答断片](../src/main/resources/templates/examples/reset-user-input-response.html)、[JavaScript](../src/main/resources/static/js/reset-user-input.js)、[テスト](../src/test/java/com/example/htmx/ResetUserInputTests.java)、[検証記録](../verification/reset-user-input.md)。
- 正常なメモをPOSTすると、htmxはサーバー生成のメモ断片をDOMの一覧先頭へ追加する。外部JavaScriptはHTTP 2xx応答時だけフォームをリセットし、422・HTTPエラー・通信断では入力値を残す。共通処理が422を差し替え可能にするため、リセット判定はhtmxの `successful` フラグではなくHTTPステータスを使う。通信中に無効化された操作へフォーカスが落ちた場合、応答後に元の操作へ戻す。
- 空欄・120文字超過は422。htmxのエラー応答はOOBでエラー領域だけを更新し、入力欄は置換しない。通常フォームではエラー時に入力値を保持し、成功時は空のフォームを含むページを返す。
- メモはサーバー状態に保存しない。htmxの一覧は現在のページDOM上だけにあり、再読み込みで消える。追加依存なし。

### No.16 の実装状況

- 実装・検証済み: `/examples/dialogs-browser`。要件 F-01、F-04、F-06〜F-09、F-25。公式例を2026-10-31に確認。第1回レビューのP0指摘（`htmx:prompt` の値変更が実送信ヘッダーに反映されない）を `htmx:configRequest` で修正。第3回レビューのP2指摘（通信失敗時にプロンプト値を再利用できない）は通常フォームへの保持と成功時消去で修正。第4回レビューのP2指摘（応答待ち中に編集した新しい値も消去される）は入力変更版を要求単位で記録して修正し、ブラウザーで応答処理直前の入力変更を確認。PR #23 作成後の第8回・第9回レビューで指摘されたダイアログ順序の説明は、同梱htmxの実順である「入力→確認」に修正。第10回レビューのP2指摘（422再表示で確認チェックが保持されない）は入力状態を反映し、成功時に解除するよう修正。第11回レビューのP2指摘（確認拒否時に通常フォームの値が更新される点と説明の不一致）は通信表で明記。第2回・第5回・第6回は指摘なし。Actions の `test` は実行ステップ開始前に失敗し、ログがないため原因を特定できない。
- [Controller](../src/main/java/com/example/htmx/examples/dialogsbrowser/DialogsBrowserController.java)、[画面](../src/main/resources/templates/examples/dialogs-browser.html)、[応答断片](../src/main/resources/templates/examples/dialogs-browser-response.html)、[補助JavaScript](../src/main/resources/static/js/dialogs-browser.js)、[テスト](../src/test/java/com/example/htmx/DialogsBrowserTests.java)、[検証記録](../verification/dialogs-browser.md)。
- `hx-prompt` の入力値を `HX-Prompt` ヘッダーから受け取り、`htmx:configRequest` でUTF-8 URIエンコードした後、Controllerでデコードし、空欄・100文字上限を検証する。`hx-confirm` / `hx-prompt` のキャンセルでは要求を送らない。表示値はThymeleafでエスケープし、保存しない。通常フォームでは入力値と確認欄を検証する。
- 統合ブラウザーの自動操作ではネイティブ `prompt()` が未対応。プロンプト・確認関数の置換で要求・キャンセル・422の通信経路を確認したが、ネイティブダイアログの実操作は未確認。追加依存なし。

### No.06 の実装状況

- マージ済み: `/examples/lazy-loading`。要件 F-01、F-04、F-06〜F-09、F-17。公式ページ再確認: 2026-10-31。[#15](https://github.com/kazu-u/htmx_sample_project/pull/15) のマージとmain取り込みを2026-10-31に確認。独立レビュー2回、最終指摘なし。
- [Controller](../src/main/java/com/example/htmx/examples/lazyloading/LazyLoadingController.java)、[画面](../src/main/resources/templates/examples/lazy-loading.html)、[テスト](../src/test/java/com/example/htmx/LazyLoadingTests.java)、[検証記録](../verification/lazy-loading.md)。
- 初期説明を先に返し、loadで固定3件の架空集計を取得する。外枠・手動リンクを残し、表だけを置換するため初回取得を繰り返さない。
- 自動・手動取得を外枠で同期し、重複要求を捨てる。HTTPエラー・通信断・10秒のタイムアウト時は表示を残して通知し、手動で再試行する。通常の集計URLは全体ページを返し、自動取得しない。
- 公式例の画像・フェードに代え、標準meterと数値の表を採用。GETは固定データの参照だけで、DB・外部API・ジョブ・業務状態・追加依存・意図的なサーバー待機なし。

### No.03 の実装状況

- 実装済み: `/examples/click-to-load`。要件 F-01、F-04、F-06〜F-09、F-11。
- [Controller](../src/main/java/com/example/htmx/examples/clicktoload/ClickToLoadController.java)、[画面](../src/main/resources/templates/examples/click-to-load.html)、[テスト](../src/test/java/com/example/htmx/ClickToLoadTests.java)
- 8件の架空データを3件ずつ表示し、htmx要求は次ページ分だけを返す。最後のページでは読み込み操作を返さない。通常フォームでは指定ページまでをページ全体で再表示する。
- DB・セッション状態・追加ライブラリなし。ページ番号は範囲検証し、通信中の重複操作はクライアントでも抑止する。
- 実行環境・確認済みの動作と未確認事項は [検証結果](../verification/click-to-load.md) に記録する。

### No.01 の実装状況

- 実装済み: `/examples/click-to-edit`。要件 F-01〜F-09。
- [Controller](../src/main/java/com/example/htmx/examples/clicktoedit/ClickToEditController.java)、[画面](../src/main/resources/templates/examples/click-to-edit.html)、[テスト](../src/test/java/com/example/htmx/ClickToEditTests.java)
- 公式例に CSRF、422 の入力検証、セッション分離、古い版の409拒否、初期化、通常フォームの代替、日本語解説を追加。
- 追加 UI ライブラリなし。名前60文字・メール254文字、セッションに1件、サーバー全体で100セッション、無通信20分で失効。
- 実行環境・確認済みの動作と未確認事項は [検証結果](../verification/click-to-edit.md) に記録する。

### No.04 の実装状況

- 実装済み: `/examples/delete-row`。要件 F-01、F-04、F-06〜F-09、F-12。
- [Controller](../src/main/java/com/example/htmx/examples/deleterow/DeleteRowController.java)、[状態](../src/main/java/com/example/htmx/examples/deleterow/DeleteRowState.java)、[画面](../src/main/resources/templates/examples/delete-row.html)、[テスト](../src/test/java/com/example/htmx/DeleteRowTests.java)
- 4件の架空データをセッション単位で保持し、DELETE の空応答で対象行を削除する。標準フォームは CSRF と確認チェック付き POST を使い、初期化も用意する。
- 公式例の行差し替えに加えて、CSRF、セッション分離、通常フォーム代替、エラー表示、削除後のフォーカス復帰を扱う。
- 実行環境・確認済みの動作と未確認事項は [検証結果](../verification/delete-row.md) に記録する。

### No.05 の実装状況

- 実装済み: `/examples/edit-row`。要件 F-01、F-04〜F-09、F-13。公式ページ再確認: 2026-10-29。
- [Controller](../src/main/java/com/example/htmx/examples/editrow/EditRowController.java)、[状態](../src/main/java/com/example/htmx/examples/editrow/EditRowState.java)、[画面](../src/main/resources/templates/examples/edit-row.html)、[行断片](../src/main/resources/templates/examples/edit-row-row.html)、[テスト](../src/test/java/com/example/htmx/EditRowTests.java)
- 3件の架空連絡先をセッション単位で保持し、行を表示・編集フォームに差し替える。複数行を独立して編集し、他の行の入力は残す。
- 通常フォームに合わせ POST と form 属性を採用し、CSRF、422、行ごとの版照合、初期化、フォーカス復帰を追加。ContactForm の入力制約を再利用する。
- 公式例の単一行編集・SweetAlert・インライン JavaScript は使わない。追加依存なし。名前60文字・メール254文字、セッション3件・無通信20分。
- 一覧の版も照合するため、htmx で行を保存した後の初期化は再読み込みが必要。未保存の全入力も初期化で破棄することを画面に明示する。
- 実行環境・確認結果と未確認事項は [検証結果](../verification/edit-row.md) に記録する。

### No.07 の実装状況

- 実装済み: `/examples/inline-validation`。要件 F-01、F-04、F-06〜F-09、F-14。公式ページ再確認: 2026-10-29。
- [Controller](../src/main/java/com/example/htmx/examples/inlinevalidation/InlineValidationController.java)、[フォーム](../src/main/java/com/example/htmx/examples/inlinevalidation/ValidationForm.java)、[画面](../src/main/resources/templates/examples/inline-validation.html)、[テスト](../src/test/java/com/example/htmx/InlineValidationTests.java)
- メールを変更して欄を離れると検証結果だけを差し替える。入力は置換せず、変更前の検証応答も反映しない。全体送信は過去の結果に頼らず、名前・メールをサーバーで再検証する。
- 公式例の固定メールだけを受け付ける仕様ではなく形式検証を採用。名前60文字・メール254文字、CSRF、422、通常フォーム代替、日本語解説を追加。追加依存なし。
- 業務状態を保存せず、登録・メール送信・重複照会は行わない。再読み込みで空のフォームに戻す。CSRF セッションの寿命・上限は既存構成に従う。
- 実行環境・確認結果と未確認事項は [検証結果](../verification/inline-validation.md) に記録する。

### No.09 の実装状況

- 実装済み: `/examples/active-search`。要件 F-01、F-04、F-06〜F-09、F-15。公式ページ再確認: 2026-10-29。
- [Controller](../src/main/java/com/example/htmx/examples/activesearch/ActiveSearchController.java)、[検索条件](../src/main/java/com/example/htmx/examples/activesearch/SearchForm.java)、[画面](../src/main/resources/templates/examples/active-search.html)、[補助JS](../src/main/resources/static/js/active-search.js)、[テスト](../src/test/java/com/example/htmx/ActiveSearchTests.java)
- 名前・チーム・メールを単一文字列で部分一致検索。英字の大小は区別せず前後空白を除く。空欄は全8件、0件も200、60文字超過は422。入力待機500ms、変換終了後の検索、最新要求への切り替え、古い200/422の抑止、通常GETフォームを扱う。
- 公式例のPOST・条件式・load取得とは異なり、参照GET・外部JS・サーバーでの初期結果を採用。入力DOMを置換せず、フォーカスを保持する。追加依存なし。
- サーバーに検索履歴・業務状態は保存しないが、検索語はURL・ブラウザー履歴・アクセスログ等に残り得る。架空データだけを使う。検証結果と未確認事項は [検証記録](../verification/active-search.md) を参照。

当初の公式例と A09 は完了しています。追加サンプルの着手順は C01 → C02 → C03 → C04 → C05 とします。順序を変更する必要が生じた場合は、利用者に確認してから台帳を更新します。
公式ページと異なる方法で実装した場合は、学べる概念・変更理由・挙動の差を各画面の解説に記録します。

## 本プロジェクトで追加する候補

以下は公式一覧を補う案です。A09 は公式例の実装後に行うタスクとして採用し、仕様合意と実装を完了しました。A01〜A08 の独立画面としての採否・実装範囲は未決定です。
「全候補を必ず個別画面として作る」という決定ではありません。
エラー表示・CSRF・入力保持などの共通品質は、候補の着手を待たず最初の実装から適用します。

| ID | テーマ | 確認したいこと |
| --- | --- | --- |
| A01 | 通信・サーバーエラー | 400 / 403 / 404 / 409 / 422 / 500、タイムアウト、オフライン、復帰 |
| A02 | 処理中表示と二重送信 | インジケーター、ボタン制御、連続操作時の整合性 |
| A03 | 検索・ページング・履歴 | URL の共有、直接アクセス、戻る・進む、ページ全体と断片の出し分け |
| A04 | CSRF と入力の安全性 | トークンの送信・拒否、HTML として解釈されない利用者入力 |
| A05 | 入力エラー後の復帰 | 値を残す、項目別表示、フォーカス、修正後の再送信 |
| A06 | アクセシビリティ | キーボード、読み上げ通知、動きを減らす設定、モバイル表示 |
| A07 | 状態の初期化・失効 | サンプルを初期状態に戻す、セッション切れ、別セッションとの分離 |
| A08 | htmx が使えない場合 | 標準フォームやリンクによる代替、対応できない操作の明示 |
| A09 | ダッシュボード部品の定期更新 | htmx のポーリングで件数・状態・更新時刻を部分更新。複数部品、一時停止・再開、手動更新、通信失敗からの復帰 |

### A09: ダッシュボード部品の定期更新

**マージ済み。** 注文・在庫の2カードを個別に5秒間隔で取得し、表示中の累計5分を自動更新の上限とします。利用者の一時停止中と非表示タブでは取得時間を計らず、自動要求も送りません。ブラウザー確認・独立レビュー後、PR #31 で2026-11-01に main へマージされました（Actions `test` 成功）。

htmx 標準の [hx-trigger のポーリング](https://htmx.org/attributes/hx-trigger/#polling) を使うと、指定した間隔で HTML 断片を取得し、カードや一覧などの部品だけを更新できます。
React、追加ライブラリ、SSE / WebSocket は不要です。サーバーからのプッシュではなく、ブラウザーから繰り返し取得する方式です。

注文カードのポーリング要素は画面に残し、取得した値だけを差し替えます。

```html
<article data-dashboard-card
         hx-get="/examples/dashboard/summary/orders"
         hx-trigger="every 5s, dashboard-refresh"
         hx-target="#orders-content"
         hx-swap="innerHTML"
         hx-sync="closest [data-dashboard-card]:drop">
  <div id="orders-content">注文の概要</div>
</article>
```

このサンプルは注文・在庫を別々に取得します。個別取得は一方の失敗を他方へ波及させず、必要なカードだけを更新できますが、要求数はカードごとに増えます。一括取得は要求をまとめてスナップショットをそろえやすい一方、不要なデータも送られ、一つの失敗が複数カードに影響します。
取得先はカード内部の HTML だけを返し、ポーリング要素を応答へ入れ子にしないため、更新ループを作りません。
この属性指定だけで、以下の停止・復帰・エラー制御まで実現できるわけではありません。必要な補助処理もサンプルに含めます。

学習内容・確認項目:

- 架空の件数・状態・サーバーの取得時刻を、DB・外部 API なしで表示する。実データではないことを明記する。
- 複数カードを個別に取得する方法と、一括取得して複数領域へ反映する方法の負荷・整合性の違いを説明する。
- GET は参照のみとし、件数の変化は時刻等から算出する模擬データで表現する。読み取り回数によって業務状態を進めない。
- 最終取得成功時刻と通信状態を表示し、失敗時は前回の値を残しつつ古い情報であることを明示する。
- 利用者が自動更新を一時停止・再開でき、手動でも更新できる。
- 非表示タブでの取得を抑え、表示復帰時に再取得する。`allowEval: false` を維持するため、JavaScript 条件式を含むトリガーフィルターではなく、外部 JavaScript のイベント処理等で制御する。
- 遅い応答時に要求をため込まず、定期取得と手動取得の競合で古い結果が新しい結果を上書きしない。
- タイムアウト・連続失敗時は無制限の即時再試行を避け、再試行間隔・停止条件・利用者への通知を決める。
- ページ離脱や部品の除去で取得を停止し、再表示時もポーリングやイベント登録が重複しない。
- 更新対象に入力中のフォームを含めず、フォーカスを奪わない。高頻度の読み上げ通知を避け、重要な状態変化だけを通知する。

公式 No.10「Progress Bar」も定期取得を使いますが、A09 はジョブの完了待ちではなく、継続的な情報表示・鮮度・更新制御を学ぶ別の候補です。

### 追加サンプル計画（優先順位合意済み）

htmx の活用範囲を広げ、実開発で参照できるサンプルを増やす計画です。2026-11-01に利用者と C01 → C02 → C03 → C04 → C05 の優先順位を合意しました。各サンプルの具体的な仕様・要件・検証範囲は着手前に合意します。C05 は htmx SSE 拡張2.2.4の採用と方針例外を含めて合意済みで、他の依存追加・方針変更まで承認済みとは扱いません。C01 は PR #33、C02 は PR #34、C03 は PR #35、C04 は F-36 として [PR #36](https://github.com/kazu-u/htmx_sample_project/pull/36) で main にマージ済みです。C05 は F-37 として検証・第6回独立レビュー済み、[PR #37](https://github.com/kazu-u/htmx_sample_project/pull/37)の Actions `test` も成功です。

| ID | テーマ | 学べること・既存例との差 |
| --- | --- | --- |
| C01 | アクセシブルな候補選択（Autocomplete / Combobox） | サーバー描画の候補、IME中の入力、キーボード選択、ARIA、古い応答の抑止。検索結果一覧ではなく、候補を選んでフォーム値に反映する操作を扱う |
| C02 | URL 駆動の絞り込み・並べ替え・ページング表 | 条件をクエリ文字列に保持し、共有可能なURL、直接アクセス、戻る・進む、ページ全体と部分応答を整合させる。既存の Active Search / Click To Load を組み合わせるだけにせず、条件と履歴の同期を主題にする |
| C03 | 複数ステップフォーム | サーバー側で段階的に検証し、戻る・進む、入力保持、確認画面、セッション期限切れやリセットを扱う。セッション単位の状態寿命・CSRF・再送信を明示する |
| C04 | 取り消せる削除・操作 | 5件の固定架空タスクをセッション単位で扱い、削除待ちから30秒以内の取り消し、期限後の論理確定、状態版による二重操作・古い画面の拒否を示す。GETを読み取り専用に保ち、バックグラウンドジョブなしで状態寿命と補償操作を学ぶ |
| C05 | サーバーイベントによるライブ通知（SSE） | 定期取得とサーバー起点のイベント配信を比較する。固定架空通知の有限ストリームを使い、HTMLイベント差し替え、`Last-Event-ID` による再開、完了・接続失敗時の停止、接続上限と切断時の後始末を学ぶ。個別合意済みの範囲で htmx SSE 拡張 2.2.4 をローカル配信する |

### C01 の実装・検証手順

1. 合意済み仕様を F-33 として定義し、実装・サーバーテスト・画面解説を用意した。対象は架空の社内担当者12件、名前・部署検索、入力後300ms、最大10件、自由入力なし、JavaScriptなしの通常フォーム代替。
2. 実ブラウザーで候補差し替え、CompositionEvent後の検索、上下矢印・Enter・Escape、選択とサーバー検証、JavaScriptなしの通常フォームを確認した。実IME入力と遅延応答での古い候補抑止は未確認事項として残す。
3. 確認結果と未確認事項を [C01 検証記録](../verification/accessible-combobox.md) に記録し、独立レビュー後に [PR #33](https://github.com/kazu-u/htmx_sample_project/pull/33) を作成した。PR #33 は2026-11-01に main へマージ済み。
4. C01完了後は C02 以降の具体仕様を合意してから着手する。C01〜C05 の計画番号を要件 ID の代用にしない。

### C02 の合意済み仕様

1. 架空の配送注文30件を使い、注文番号・顧客名の検索、配送状態の絞り込み、注文番号・顧客名・注文日のソート、1ページ5件のページングを行う。
2. `keyword`、`status`、`sort`、`direction`、`page` を GET クエリに含め、直接アクセス・再読み込み・URL共有・ブラウザー履歴で同じ条件を再現する。
3. htmx 要求では一覧領域のHTML断片、通常要求と履歴復元要求ではページ全体を返す。JavaScript無効時は通常GETフォームとリンクを使う。
4. 検索語は60文字以内。固定データのみで状態保存・DB・外部API・追加依存を使わない。C01〜C05 の計画番号を要件 ID の代用にしない。

検証結果は [C02 検証記録](../verification/url-driven-table.md) を参照。PR #34 は2026-11-03に main へマージ済み。

### C05 の合意済み仕様

1. `/examples/live-notifications` を開くと htmx SSE 拡張が同一オリジンの `/examples/live-notifications/stream` へGET接続する。架空の通知5件を3秒間隔でHTMLイベントとして一覧へ追記し、完了イベントで接続を閉じる。
2. 通知は固定データとし、DB・外部API・セッション・業務状態は使わない。イベントIDは1〜5、`Last-Event-ID` と EventSource 再作成時の `lastEventId` クエリを範囲検証し、最後に受信したイベントの次から再開する。
3. アプリ全体の接続上限は8、ストリームのタイムアウトは20秒。切断・完了・停止・アプリ終了時に予約タスクと接続枠を解放する。接続失敗は5回まで再試行し、上限で通知して停止、ページ再読み込みを案内する。
4. htmx SSE 拡張2.2.4を0BSDライセンスと配布チェックサム付きでローカル配信する。CSP・htmxの安全設定を維持し、JavaScript無効時は通常ページと制約の説明を表示する。

## 台帳の更新方法

実装ごとに以下を記録します。

- 状態: 未実装 / 実装中 / 実装済み
- 実装先と画面 URL
- 確認した JDK・ライブラリ・ブラウザーのバージョン
- 確認した正常系・異常系と、未確認事項
- 公式例との差分、追加依存の有無、既知の制約

行を「実装済み」に変えるのは、[実装・解説ガイド](implementation-guide.md) の完了条件を満たした後とします。
