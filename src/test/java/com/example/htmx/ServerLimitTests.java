package com.example.htmx;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;
import org.apache.catalina.Context;
import org.apache.catalina.session.StandardManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** MockMvc では通らない Tomcat の要求サイズ・件数・セッション上限を実 HTTP で確認する。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServerLimitTests {
    @Autowired ServletWebServerApplicationContext application;

    @Test
    void embeddedServerEnforcesConfiguredLimits() throws Exception {
        var server = (TomcatWebServer) application.getWebServer();
        var context = (Context) server.getTomcat().getHost().findChildren()[0];
        var manager = (StandardManager) context.getManager();
        assertThat(context.getSessionTimeout()).isEqualTo(20);
        assertThat(manager.getMaxActiveSessions()).isEqualTo(100);
        assertThat(server.getTomcat().getConnector().getMaxPostSize()).isEqualTo(16 * 1024);
        assertThat(server.getTomcat().getConnector().getMaxParameterCount()).isEqualTo(20);
        String base = "http://127.0.0.1:" + server.getPort() + "/examples/click-to-edit";

        try (var client = HttpClient.newHttpClient()) {
            var staleEdit = client.send(HttpRequest.newBuilder(URI.create(base + "/contact/edit")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(staleEdit.statusCode()).isEqualTo(200);
            var staleCookieMatcher = Pattern.compile("JSESSIONID=([^;]+)")
                    .matcher(staleEdit.headers().firstValue("Set-Cookie").orElseThrow());
            assertThat(staleCookieMatcher.find()).isTrue();
            String staleSessionId = staleCookieMatcher.group(1);
            var staleTokenMatcher = Pattern.compile("<input[^>]*name=\"_csrf\"[^>]*value=\"([^\"]+)\"")
                    .matcher(staleEdit.body());
            assertThat(staleTokenMatcher.find()).isTrue();
            String staleCsrfToken = staleTokenMatcher.group(1);

            for (int i = 1; i < 100; i++) {
                assertThat(client.send(HttpRequest.newBuilder(URI.create(base)).GET().build(),
                        HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(200);
            }
            assertThat(manager.getActiveSessions()).isEqualTo(100);
            var full = client.send(HttpRequest.newBuilder(URI.create(base)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(full.statusCode()).isEqualTo(503);
            assertThat(full.body()).contains("セッション上限の可能性があります")
                    .contains("ローカルサーバーを再起動してください");
            assertThat(manager.getActiveSessions()).isEqualTo(100);

            var expiredSession = manager.findSession(staleSessionId);
            assertThat(expiredSession).isNotNull();
            expiredSession.expire();
            assertThat(manager.getActiveSessions()).isEqualTo(99);

            assertThat(client.send(HttpRequest.newBuilder(URI.create(base)).GET().build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(200);
            assertThat(manager.getActiveSessions()).isEqualTo(100);

            var stalePost = HttpRequest.newBuilder(URI.create(base + "/contact"))
                    .header("Cookie", "JSESSIONID=" + staleSessionId)
                    .header("X-CSRF-TOKEN", staleCsrfToken)
                    .header("Accept", "text/html")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("revision=stale&name=name&email=x%40example.com"))
                    .build();
            var staleResponse = client.send(stalePost, HttpResponse.BodyHandlers.ofString());
            assertThat(staleResponse.statusCode()).isEqualTo(503);
            assertThat(staleResponse.body()).contains("セッション上限の可能性があります");
            assertThat(manager.getActiveSessions()).isEqualTo(100);
        } finally {
            for (var session : manager.findSessions()) session.expire();
        }

        try (var client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build()) {
            var edit = client.send(HttpRequest.newBuilder(URI.create(base + "/contact/edit")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(edit.statusCode()).isEqualTo(200);
            var token = Pattern.compile("<input[^>]*name=\"_csrf\"[^>]*value=\"([^\"]+)\"")
                    .matcher(edit.body());
            assertThat(token.find()).isTrue();
            var revision = Pattern.compile("name=\"revision\"[^>]*value=\"([^\"]+)\"").matcher(edit.body());
            assertThat(revision.find()).isTrue();
            String valid = "name=valid&email=x%40example.com&revision=" + revision.group(1);
            String twenty = valid + "&extra=x".repeat(17);
            var accepted = sendForm(client, base, token.group(1), twenty);
            assertThat(accepted.statusCode()).isEqualTo(200);
            assertThat(sendForm(client, base, token.group(1), twenty + "&extra=x").statusCode()).isEqualTo(400);

            var exactSize = sendForm(client, base, token.group(1), "ignored=" + "x".repeat(16 * 1024 - 8));
            assertThat(exactSize.statusCode()).isEqualTo(422);
            assertThat(sendForm(client, base, token.group(1),
                    "ignored=" + "x".repeat(16 * 1024 - 7)).statusCode()).isEqualTo(413);
        }
    }

    private HttpResponse<String> sendForm(HttpClient client, String base, String token, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + "/contact")).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("X-CSRF-TOKEN", token).header("HX-Request", "true")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
