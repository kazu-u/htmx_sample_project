# ADR 0001: 最小構成の初期依存

- 状態: 採用
- 決定日: 2026-10-27
- 対応要件: F-01、F-03、F-04、F-08

## 判断

Spring MVC と Thymeleaf で HTML を返し、htmx で部分更新する。
DB、ORM、外部 API、SPA、Node.js ビルド、UI ライブラリは追加しない。

| 依存・ツール | 採用版・管理 | 必要性 | ライセンス |
| --- | --- | --- | --- |
| Eclipse Temurin JDK | 25.0.5+8 | OpenJDK ベースの Java 25。利用者承認済み。既存 JDK やグローバル PATH を変更せず検証する | GPLv2 + Classpath Exception |
| Spring Boot | 4.1.2 | Initializr が提示する最新 GA の4系。SNAPSHOT / RC は使わない | Apache-2.0 |
| spring-boot-starter-webmvc | Boot 管理 | HTTP と Spring MVC | Apache-2.0 |
| spring-boot-starter-thymeleaf | Boot 管理 | エスケープ可能な HTML テンプレート | Apache-2.0 |
| spring-boot-starter-validation | Boot 管理 | 名前・メールのサーバー検証 | Apache-2.0（Hibernate Validator は Apache-2.0） |
| spring-boot-starter-security | Boot 管理 | CSRF、保護ヘッダー、公開範囲の明示 | Apache-2.0 |
| spring-boot-starter-webmvc-test / spring-boot-starter-security-test | Boot 管理・テストのみ | 実際の MVC・Security・テンプレートを含むテスト | Apache-2.0（JUnit は EPL-2.0、AssertJ は Apache-2.0） |
| htmx | 2.0.11 | フロントエンドの部分更新。追加依存なし | 0BSD。配布ライセンスを同梱 |
| Maven / Wrapper Plugin | 3.9.16 / 3.3.4 | 固定したビルド手順を提供する | Apache-2.0 |

Spring 関連とテストの推移的依存は Boot BOM に従う。バージョンを個別に上書きしない。
プロジェクト自体の配布ライセンスは未決定であり、第三者ライセンスとは区別する。

## 更新と取得

- [Initializr metadata](https://start.spring.io/metadata/client) で GA を確認した。
- [Adoptium API](https://api.adoptium.net/v3/assets/latest/25/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse) で Windows x64 GA を確認した。
- [Temurin サポート](https://adoptium.net/support/) は25系を少なくとも2031年9月まで提供する方針。商用 SLA の保証ではない。
- htmx は公式リポジトリのタグ付き配布物を取得し、ハッシュとライセンスを記録する。
- Maven は公式配布物の SHA-512 を照合した上で SHA-256 を計算し、Wrapper が対応する `distributionSha256Sum` に固定する。更新は公式ハッシュとの照合とビルド検証を伴う。

## CI の依存

GitHub Actions の `actions/checkout` と `actions/setup-java` をタグではなく取得時のコミット SHA で固定して使用する。
いずれも MIT。ソース取得と Temurin 25 のセットアップのために必要であり、アプリの実行時依存ではない。
CI の Java 25 はセキュリティ更新を取り込む系列指定とし、ローカル検証の厳密版と区別して実行ログに記録する。
権限は `contents: read`、資格情報はチェックアウトに永続化しない。
2026-10-27 に GitHub API のタグ参照で `actions/checkout` v6 = `d23441a48e516b6c34aea4fa41551a30e30af803`、
`actions/setup-java` v5 = `b6effb05e454b25005698d916606bdc6ffcbf961` を確認した。
feature ブランチは PR イベント、main は push イベントで検証し、同じ PR の push と pull_request による重複実行を避ける。
