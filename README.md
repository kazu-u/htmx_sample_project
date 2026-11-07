# Spring Boot + htmx 日本語サンプル集

Spring Boot と htmx を初めて学ぶ方向けの、日本語解説付きサンプル集です。Java の基本構文を知っている方を対象に、HTML の操作が HTTP 通信、サーバー処理、画面更新につながる流れを動かしながら学べます。

公式の [htmx Examples](https://htmx.org/examples/) をもとにした23例と、ダッシュボードの定期更新や複数ステップフォームなどの追加例6件を収録しています。各例の内容・実装状況は[サンプル台帳](docs/sample-catalog.md)を参照してください。

## 学べること

- Spring MVC と Thymeleaf で HTML 全体や部分更新用の断片を返す方法
- `hx-*` 属性と HTTP メソッド、URL、更新対象の関係
- 入力検証、CSRF 対策、エラー応答、処理中表示
- キーボード操作、フォーカス、アクセシビリティへの配慮
- htmx による検索、ページング、ファイル送信、ポーリング、SSE など

DB や外部 API は使わず、架空データでローカル実行できます。可変データを扱う例ではセッション内のメモリを使います。学習用のサンプルであり、本番運用向けの認証・認可、永続化、監視、インフラ設定は含みません。

## 実行環境

| 項目 | バージョン・構成 |
| --- | --- |
| Java | 25 |
| Spring Boot | 4.1.2 |
| htmx | 2.0.11 |
| htmx SSE extension | 2.2.4（SSE の例で使用） |
| Maven | Wrapper 3.9.16 |

## 起動方法

Java 25 の JDK と、初回の Maven・依存取得に必要なネットワーク接続を用意してください。Maven を個別にインストールする必要はありません。

### Windows（PowerShell）

```powershell
.\mvnw.cmd spring-boot:run
```

### Linux / macOS

```sh
sh mvnw spring-boot:run
```

ブラウザーで <http://127.0.0.1:8080/> を開いてください。停止するには起動したターミナルで `Ctrl+C` を押します。

## テスト

### Windows（PowerShell）

```powershell
.\mvnw.cmd --batch-mode verify
```

### Linux / macOS

```sh
sh mvnw --batch-mode verify
```

## ドキュメント

- [開発・検証手順](docs/development.md) — 環境構築、起動、テスト
- [サンプル台帳](docs/sample-catalog.md) — 収録例の一覧と実装状況
- [プロジェクト方針・設計](docs/project-policy.md) — 技術構成、安全性、データ管理
- [実装・解説ガイド](docs/implementation-guide.md) — サンプルの説明・確認基準
- [要件一覧](docs/requirements.md) — 機能要件
- [検証記録](verification/) — ブラウザー確認の手順、結果、未確認事項

## ライセンス

このリポジトリ独自のソースコードと解説文は [MIT License](LICENSE) で公開しています。

同梱している htmx と htmx SSE extension は、それぞれの配布物に付属する 0BSD ライセンスに従います。詳細は [htmx の配布情報](src/main/resources/static/vendor/htmx/README.txt) と [SSE extension の配布情報](src/main/resources/static/vendor/htmx-ext-sse/2.2.4/README.txt) を確認してください。その他の依存ライブラリには各権利者のライセンスが適用されます。

## 参考資料

- [htmx Examples](https://htmx.org/examples/)
- [htmx Documentation](https://htmx.org/docs/)
- [Spring Boot System Requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Security: CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
