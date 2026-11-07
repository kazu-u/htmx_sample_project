# 開発・検証手順

## 作業開始・中断・再開

1. ブランチと作業ツリーを確認する。利用者の変更を破棄せず、対象外のファイルを混ぜない。
2. [サンプル台帳の進捗管理](sample-catalog.md#進捗管理再開時の入口)で現在のタスク・次の操作・未解決事項を確認する。
3. 対象が決まっていれば関連する要件・方針・実装を読んで続ける。対象が未決定なら利用者に確認し、履歴から推測して着手しない。
4. 中断時や作業段階の完了時に、台帳の状態・完了チェック・再開メモを更新する。検証結果やレビュー判断の詳細は既存の検証記録・PR に記録する。
5. 次回の開始時にマージを確認したら、台帳を「マージ済み」に更新する。実装・検証・レビューの完了と、マージの完了を区別する。

台帳で再開内容が分かる場合は、過去の会話やコミット履歴を調べ直さない。

利用者から開発を依頼された場合は、実装・関連文書更新・ローカル検証に続けて、最新差分の独立レビュー・コミット・PR 作成まで実施する。通常は段階ごとの再確認で停止しない。
未解決の指摘や判断が必要な仕様、権限・実行環境の制約がある場合は状況を記録して利用者に確認する。マージは自動で行わず、人の承認に残す。詳細は [レビュー方針](review-policy.md) に従う。

## 必要な環境

- OpenJDK ベースの Java 25。初回検証は Eclipse Temurin 25.0.5+8（Windows x64）
- 初回は依存・Maven のダウンロードが可能なネットワーク
- Maven の個別インストールは不要。Wrapper は Maven 3.9.16 を使う

JDK の ZIP を展開した場合、PowerShell で `JAVA_HOME` をその JDK のルートへ設定し、`bin` をそのプロセスの PATH に追加する。
プロジェクト内に JDK を置いたり、各開発者の絶対パスを設定ファイルへコミットしたりしない。

```powershell
# 実際に展開した JDK のパスへ置き換える。
$env:JAVA_HOME = 'C:\tools\jdk-25.0.5+8'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version
.\mvnw.cmd --version
.\mvnw.cmd --batch-mode verify
.\mvnw.cmd spring-boot:run
```

ブラウザーで `http://127.0.0.1:8080/` を開く。停止は起動したターミナルで Ctrl+C。
Linux / macOS は Java 25 を設定した上で `sh mvnw --batch-mode verify` / `sh mvnw spring-boot:run` を使う。
Windows 以外の実行は CI の結果も確認する。

## 構成の読み方

- `SampleApplication`: 起動と自動設定。認証ユーザーの自動生成は、この認証なしサンプルでは除外する。
- `SecurityConfig`: 全アクセス許可の明示、CSRF、CSP。認証なしと保護なしを区別する。
- `SessionLimitConfig`: Tomcat のメモリ内セッションを全体で100個に制限し、永続化しない。上限到達時は503を返して既存セッションの利用・失効待ち・再起動を案内する。
- `SessionCapacityExceededFilter`: CSRF など Controller より前のフィルターで発生する上限到達も503のエラーページへつなぐ。
- `ClickToEditController`: ページ全体と断片の選択、入力検証、保存・初期化。
- `ClickToLoadController`: 固定データをページ番号で切り出し、通常ページまたは次ページの行断片を返す。
- `EditRowController` / `EditRowState`: 3行の独立編集、行ごとの版照合、一覧の初期化。入力制約は既存の `ContactForm` を再利用する。
- `InlineValidationController` / `ValidationForm`: メール単独検証と送信時の全項目再検証。同じメール制約を検証グループで共有し、業務状態は保存しない。
- `ActiveSearchController` / `SearchForm`: 固定8件の名前・チーム・メールをGETで部分一致検索する。検索履歴・業務状態は保存しない。
- `AccessibleComboboxController` / `ComboboxForm`: 固定12件の担当者を名前・部署で検索し、最大10件の候補を返す。選択IDをサーバーで照合し、状態は保存しない。
- `OrderTableController` / `OrderTableQuery`: 架空の注文30件をURLのキーワード・配送状態・ソート・ページ番号で絞り込み、全体ページまたは一覧断片を返す。GETだけで状態を保存しない。
- `LiveNotificationsController` / `LiveNotificationStream`: C05 のSSEページと架空通知5件の有限ストリームを提供する。`Last-Event-ID` ヘッダーと再作成時の検証済みクエリカーソルから再開し、同時接続数・時間の上限、切断時のタスク解放を行う。失敗が続く接続は外部JavaScriptで停止する。
- `BulkUpdateController` / `BulkUpdateForm` / `BulkUpdateState`: セッション内の4人の有効・無効をチェック状態どおり一括保存する。未送信の行は無効とし、入力不正なら全件拒否、古い版は409で拒否する。
- `LazyLoadingController`: 初期HTMLと固定3件の集計取得を分ける。htmxには表だけを、通常アクセスには集計入りの全体ページを返す。業務状態や待機処理は持たない。
- `InfiniteScrollController`: 固定9件の架空書籍を3件ずつ取得。通常アクセスは指定ページまで累積表示し、htmxは次ページの行と操作行（最終は終了行）を返す。
- `TabsHateoasController` / `TabsJavascriptController`: No.20 はURLから選択状態をサーバーが返し、No.21 は選択した固定コンテンツだけを返す。No.21 の選択表示は外部JavaScriptが部分応答の成功後に更新する。
- `KeyboardShortcutsController`: No.22 の固定案内を GET で返す。ボタンと Alt+Shift+D は同じ要求を行い、サーバー状態は変更しない。
- `FileUploadInputController` / `UploadedTextFileValidator`: ファイル検証エラーの422フォームを返し、htmxのフォーム差し替え時には `hx-preserve` でファイル選択を維持する。No.13のアップロード例と検証制約を共有する。
- `ResetUserInputController` / `NoteForm`: メモの入力をサーバー検証し、成功応答だけを受けた外部JavaScriptがフォームを初期化する。メモはサーバー状態に保存しない。
- `ContactForm`: 名前60文字、メール254文字までの入力と検証。UTF-16 の文字列長を用いる。
- `ContactState`: セッション単位の1件。同期処理と版照合で競合を拒否する。
- `click-to-edit.html`: 全体ページと `contact` フラグメント、日本語の学習説明。
- `click-to-load.html` / `click-to-load-rows.html`: 全体ページと行フラグメント、日本語の学習説明。通常フォームでも指定ページまでを表示する。
- `edit-row.html` / `edit-row-row.html`: 一覧と表示・編集の行断片。別セルの入力は標準 HTML の form 属性で操作セルのフォームに関連付ける。
- `inline-validation.html`: メールの結果だけの差し替えと全体フォームの422。入力要素は単独検証で置換せず、古い検証結果は共通 JavaScript で抑止する。
- `active-search.html` / `active-search.js`: 通常GETフォームと検索結果断片。外部JavaScriptで500ms待機・IME・古い応答を扱い、入力は置換しない。
- `accessible-combobox.html` / `accessible-combobox.js`: 300ms待機で候補断片を取得し、IME・古い応答・ARIA combobox状態・キーボード選択を扱う。JavaScriptなしでは通常GETフォームとselectで選択する。
- `url-table.html` / `url-driven-table.js`: フィルター・ソート・ページリンクに同じ GET URL を使い、htmx の履歴復元と通常遷移を一致させる。差し替え後の操作フォーカスも復帰する。
- `MultiStepFormController` / `TravelApplicationForm` / `TravelApplicationState`: 架空の出張申請を3段階で処理する。段階ごとの検証、セッション下書き、revision による古い画面の拒否、確認後の完了表示を行い、外部保存はしない。
- `multi-step-form.html`: 通常 POST と htmx の段階遷移、422 の入力保持、確認・完了・リセット、日本語の通信表とセッション期限の説明を提供する。
- `MultiStepFormTests` / [C03 検証記録](../verification/multi-step-form.md): F-35 の段階別検証・状態遷移をサーバーで確認し、ブラウザー結果と未確認事項を記録する。
- `bulk-update.html`: 全行のチェック欄をフォームで囲み、保存済み状態と版を含む `bulk-panel` を差し替える。共通 JavaScript が422とフォーカス、未保存の変更通知を扱う。
- `lazy-loading.html`: 外枠のloadで一度だけ集計を取得し、中身だけを置換する。自動と手動は同じ外枠で同期し、リンク・フォーカスを残す。通常リンクでも集計を表示できる。
- `infinite-scroll.html` / `infinite-scroll-rows.html`: スクロール枠のintersect onceと手動submitで取得し、操作行を置換する。共通JavaScriptが手動操作時のみフォーカスを復帰し、自動取得では奪わない。
- `tabs-hateoas.html` / `tabs-javascript.html`: 2つのタブ例を比較する。HATEOAS 版は選択状態と内容をサーバーから返し、JavaScript 版は内容だけを差し替えて外部JavaScriptがARIA選択状態を管理する。
- `tabs-hateoas.js` / `tabs-javascript.js`: 両例のキーボード移動を補助する。JavaScript 版は初期化時にロービング `tabindex` を設定し、要求元タブをXHRに紐付けて成功応答後だけ選択状態を切り替える。JavaScript 無効時は全リンクがフォーカス可能なまま残る。
- `keyboard-shortcuts.html` / `keyboard-shortcuts.js`: No.22 の画面、通常 GET フォームと断片差し替えを定義する。外部JavaScriptは Alt+Shift+D を検出し、入力中・編集可能領域・開いたダイアログ内では動作させない。
- `WebComponentsController` / `web-components.html` / `web-components.js`: No.27 の固定概要カードを表示する。カスタム要素の Shadow DOM にテンプレートを複製してから `htmx.process` を呼び、カード単位の差し替えとイベント通知を行う。通常リンクは全ページを返す。
- `web-components.css`: No.27 の Shadow DOM 内で読み込む外部スタイル。CSP の `style-src 'self'` を維持する。
- `DashboardController` / `dashboard.html` / `dashboard-cards.html` / `dashboard-polling.js`: A09 の架空注文・在庫カードを5秒ごとに個別取得する。画面表示中の累計5分を上限にし、外部JavaScriptで一時停止・非表示タブ・エラー復帰を制御する。GETは時刻由来の表示値を返すだけで状態を変更しない。
- `dashboard.css`: A09 のカードを画面幅に応じて並べ、狭い画面では1列にする。
- `file-upload-input.html` / `file-upload-input.js`: 422でフォーム全体を置換し、エラー時はファイルを保持、成功時はファイル入力とタイトルをクリアする。
- `reset-user-input.html` / `reset-user-input.js`: 成功時にメモ断片を一覧へ追加し、`htmx:afterRequest` のHTTP 2xx判定後にフォームを初期化する。422では入力欄を差し替えず値を保持する。
- `fragments/sample-menu.html` / `app.css`: 実装済みサンプルを示す共通サイドメニューと、狭い画面での折り返し表示。
- `app.js`: 同一オリジンの更新時に CSRF ヘッダーを付け、422・通信失敗・フォーカスを扱う。
- `app.css`: ローカルのスタイル。htmx の注入スタイルを使わない。

要求上限は組み込み Tomcat の実 HTTP テストでも確認する。パラメーター21個は400、16KBを超えるフォームは413となる。
入力制約に反する通常サイズのフォームは、アプリが422のフォームを返す。

通常フォームでも操作できる。入力検証の仕組みを試せるよう、フォームは `novalidate` とし、必須項目・形式はサーバーで必ず検証する。
CSRF は通常フォームの hidden 項目と、htmx 用ヘッダーの両方に対応する。
保存や初期化の応答が通信断で不明になった場合、成功・失敗を推測せず、入力を控えて表示を再取得する。

キャッシュ方針は Spring Security のデフォルトを維持する。HTML と静的ファイルも `no-store` の対象とし、学習時に古いコードが残る混乱を避ける。
そのためページ再読み込みではローカルの htmx / CSS / JavaScript も再取得する。初期サンプルは小規模なローカル配信を優先しており、本番向けの静的アセットキャッシュ最適化は対象外。

## Maven の固定・更新

Wrapper は公式 Maven Wrapper Plugin 3.3.4 の `only-script` 形式で生成した。
[maven-wrapper.properties](../.mvn/wrapper/maven-wrapper.properties) に配布 ZIP の URL と SHA-256 を固定する。
Maven の本体更新は Dependabot ではなく手動で行う。

1. Maven 公式配布元で採用版と SHA-512 を確認する。
2. ZIP をダウンロードし、SHA-512 が一致することを確認する。
3. 確認済み ZIP の SHA-256 を計算し、配布 URL と `distributionSha256Sum` を更新する。
4. Wrapper がまだその配布物を持っていない独立のキャッシュで、取得・ハッシュ検証と `verify` を確認する。
5. ADR・確認結果を更新し、通常の独立レビューを受ける。

初期版3.9.16の公式 ZIP SHA-512:

```text
ed41650d42485cfc243fad22158caf9cbb5dc408ce7a09ddb94dd42a019de929ca43065bfa450612cf12bf78b5cafa3884b96c090de326ff590448c933454af3
```

Wrapper のハッシュ検証は新規ダウンロード時の処理。既存のキャッシュ内容を毎回検証する仕組みではない。

## PR 前レビュー

要件は [要件一覧](requirements.md)、運用は [レビュー方針](review-policy.md) を参照する。

1. 実装・ローカルテスト・ブラウザー確認を終え、正確な最新差分を用意する。
2. Independent Reviewer サブエージェントに、差分、目的1〜2行、要件 ID、レビュー回数だけを渡す。利用不能・開始前停止・未完了なら、同じ読み取り専用条件で新しい組み込み `code-review` サブエージェントへ自動切り替えする。切り替えの都度確認や別チャットの用意は不要。両方で完了できなければ、レビュー方針に従って相談する。
3. レビュー担当は読み取り・検索だけで確認し、変更・コマンド・Git 操作・外部サービス操作を行わない。ツールが見えるだけでは中止せず、それらを呼び出さない。
4. P0 は修正する。P1 は原則修正し、保留には利用者の判断を得る。P2 は影響を評価して修正または受け入れ、対応を PR に記録する。
5. 修正した場合は関連テストを再実行し、更新した最新差分を新しいレビューサブエージェントで再レビューする。実装経緯・コミット履歴・前回レビュー結果は次の担当に渡さない。
6. 最終レビュー完了後にコミットし、PR を作成する。代替レビュアーへの切り替え・完了回数・確認範囲は PR に記録し、停止した試行は完了に数えない。GitHub Actions が利用制限等で実行できない場合は、未実行とローカル結果を明記する。

レビュー担当はテストを実行しないため、テスト結果は実装側が検証記録・PR に記載する。レビューの自動実行スクリプト `scripts/review.sh` は未整備であり、この流れには必須ではない。
