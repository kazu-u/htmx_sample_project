# サーバーイベントによるライブ通知 検証記録

## 対象と環境

- 対象: C05「サーバーイベントによるライブ通知」、要件 F-37
- 確認日: 2026-11-03
- Java: Oracle JDK 25.0.2+10
- Spring Boot: 4.1.2
- htmx: 2.0.11
- htmx SSE 拡張: 2.2.4
- ブラウザー: VS Code 統合ブラウザー（Chromium 系、詳細バージョン未確認）

## サーバーテストと起動

- `.\mvnw.cmd --batch-mode clean verify` — 285件成功、失敗・エラー・スキップなし。`BUILD SUCCESS`。
- `java -jar .\target\htmx-samples-0.0.1-SNAPSHOT.jar --server.port=18080` — パッケージ済み JAR から起動し、ライブ通知ページが HTTP 200、SSE 応答が `text/event-stream; charset=UTF-8` であることを確認した。
- `/examples/live-notifications/stream` に `Last-Event-ID: 5` を付けた GET は `finished` イベントで終了した。
- 自動テストで、上限数の接続、実 HTTP クライアント切断後の送信失敗による枠解放、20秒タイムアウト後の枠解放を確認した。
- `gh pr checks 37` — GitHub Actions `test` 成功（1分22秒）。

## ブラウザー確認

- `/examples/live-notifications` を開き、3秒間隔で5件の日本語通知が一覧末尾へ追加され、接続状態が正常終了に変わることを確認した。
- SSE 接続を `Last-Event-ID: 2` から再開し、イベントID 3・4・5と完了イベントだけを受け取ることを確認した。使用した要求は次のとおり。
  - `curl.exe --silent --show-error --no-buffer --max-time 14 --header "Last-Event-ID: 2" http://localhost:18080/examples/live-notifications/stream`
- ブラウザーでイベントID 2 の後に再接続要求を HTTP 503 にし、拡張が EventSource を作り直す際に `?lastEventId=2` を付けることを確認した。再作成後はID 3・4・5と完了イベントを表示し、ID 1を重複表示しなかった。
- ブラウザー自動化で SSE 要求を失敗させ、5回の連続失敗後に再試行が止まり、接続状態と再読み込み案内が表示されることを確認した。

## 未確認事項

- JavaScript を無効にしたブラウザーで `<noscript>` の案内が実際に表示されることは未確認。サーバーテストで案内文がページに含まれることは確認したが、統合ブラウザーの自動化環境では JavaScript 無効の新規コンテキストを作成できなかった。
- 8件目を超えるブラウザー接続時の表示、スクリーンリーダー、モバイル表示、別ブラウザーは未確認。
