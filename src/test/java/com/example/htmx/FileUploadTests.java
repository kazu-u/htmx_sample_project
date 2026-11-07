package com.example.htmx;

import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** multipart の形式・件数・CSRF と、ファイル内容を保持しない応答を確認する。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class FileUploadTests {
    private static final String BASE = "/examples/file-upload";
    private static final long MAX_FILE_SIZE = 1024L * 1024L;

    @Autowired MockMvc mvc;

    @Autowired ServletWebServerApplicationContext application;

    @Test
    void pageExplainsMultipartUploadProgressAndLimits() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("enctype=\"multipart/form-data\"")))
                .andExpect(content().string(containsString("hx-encoding=\"multipart/form-data\"")))
                .andExpect(content().string(containsString("/js/file-upload.js")))
                .andExpect(content().string(containsString("/css/file-upload.css")))
                .andExpect(content().string(containsString("1 MiB")))
                .andExpect(content().string(containsString("サーバー側の処理進捗を表すものではありません")))
                .andExpect(content().string(containsString("aria-current=\"page\"")));
    }

    @Test
    void acceptsUtf8TextAndReturnsOnlySanitizedMetadata() throws Exception {
        String secretText = "この内容は応答に含めない";
        var file = new MockMultipartFile("files", "..\\<script>.txt", "application/octet-stream",
                secretText.getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart(BASE).file(file).with(csrf()).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("&lt;script&gt;.txt")))
                .andExpect(content().string(containsString("bytes")))
                .andExpect(content().string(containsString("保存せず")))
                .andExpect(content().string(not(containsString(secretText))))
                .andExpect(content().string(not(containsString("<script>"))))
                .andExpect(content().string(not(containsString("..\\"))))
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    void normalFormRedirectsAfterSuccessfulValidation() throws Exception {
        var file = new MockMultipartFile("files", "memo.txt", "text/plain",
                "plain text".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart(BASE).file(file).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("uploadNotice", containsString("memo.txt")));
    }

    @Test
    void missingOrMultipleFilesReturn422() throws Exception {
        mvc.perform(multipart(BASE).with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("1件選択してください")));

        var first = new MockMultipartFile("files", "first.txt", "text/plain", "one".getBytes(StandardCharsets.UTF_8));
        var second = new MockMultipartFile("files", "second.txt", "text/plain", "two".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart(BASE).file(first).file(second).with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("1件選択してください")));

        var additionalField = new MockMultipartFile("other", "second.txt", "text/plain",
                "two".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart(BASE).file(first).file(additionalField).with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("1件選択してください")));
    }

    @Test
    void rejectsWrongExtensionAndInvalidUtf8RegardlessOfDeclaredMimeType() throws Exception {
        var wrongExtension = new MockMultipartFile("files", "memo.bin", "text/plain",
                "plain text".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart(BASE).file(wrongExtension).with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("拡張子が .txt")));

        var invalidUtf8 = new MockMultipartFile("files", "memo.txt", "text/plain",
                new byte[]{(byte) 0xc3, (byte) 0x28});
        mvc.perform(multipart(BASE).file(invalidUtf8).with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("UTF-8 のテキストではありません")));
    }

    @Test
    void enforcesFileNameLengthByUnicodeCodePoint() throws Exception {
        var atLimit = new MockMultipartFile("files", "あ".repeat(116) + ".txt", "text/plain",
                "text".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart(BASE).file(atLimit).with(csrf()).header("HX-Request", "true"))
                .andExpect(status().isOk());

        var overLimit = new MockMultipartFile("files", "あ".repeat(117) + ".txt", "text/plain",
                "text".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart(BASE).file(overLimit).with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("120文字以内")));
    }

    @Test
    void normalFormShowsValidationErrorsInAFullPage() throws Exception {
        var invalid = new MockMultipartFile("files", "memo.bin", "text/plain",
                "plain text".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart(BASE).file(invalid).with(csrf()))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("拡張子が .txt")));
    }

    @Test
    void rejectsNulBytesAndOversizedFiles() throws Exception {
        var binaryText = new MockMultipartFile("files", "memo.txt", "text/plain",
                new byte[]{'a', 0, 'b'});
        mvc.perform(multipart(BASE).file(binaryText).with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422));

        var oversized = new MockMultipartFile("files", "large.txt", "text/plain",
                new byte[(int) MAX_FILE_SIZE + 1]);
        mvc.perform(multipart(BASE).file(oversized).with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(413))
                .andExpect(content().string(containsString("送信内容が上限を超えています")));
    }

    @Test
    void uploadRequiresCsrf() throws Exception {
        var file = new MockMultipartFile("files", "memo.txt", "text/plain",
                "plain text".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart(BASE).file(file).header("HX-Request", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    void embeddedServerReadsCsrfFromNormalMultipartFormBody() throws Exception {
        var server = (TomcatWebServer) application.getWebServer();
        String base = "http://127.0.0.1:" + server.getPort() + BASE;
        try (var client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build()) {
            var page = client.send(HttpRequest.newBuilder(URI.create(base)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            String csrf = metaContent(page.body(), "_csrf");
            String csrfHeader = metaContent(page.body(), "_csrf_header");
            var body = multipartBody("memo.txt", "plain text".getBytes(StandardCharsets.UTF_8), csrf);

            var accepted = upload(client, base, csrfHeader, csrf, body, false, false);
            assertThat(accepted.statusCode()).isEqualTo(302);
            assertThat(accepted.headers().firstValue("location").orElseThrow()).endsWith(BASE);

            var rejected = upload(client, base, csrfHeader, "invalid", multipartBody(
                    "memo.txt", "plain text".getBytes(StandardCharsets.UTF_8), "invalid"), false, false);
            assertThat(rejected.statusCode()).isEqualTo(403);
        }
    }

    @Test
    void embeddedServerAcceptsTheConfiguredLimitAndRejectsLargerFiles() throws Exception {
        var server = (TomcatWebServer) application.getWebServer();
        String base = "http://127.0.0.1:" + server.getPort() + BASE;
        try (var client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build()) {
            var page = client.send(HttpRequest.newBuilder(URI.create(base)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(page.statusCode()).isEqualTo(200);
            String csrf = metaContent(page.body(), "_csrf");
            String csrfHeader = metaContent(page.body(), "_csrf_header");

            Set<Path> temporaryFilesBeforeUpload = temporaryFiles();
            var exactLimitBody = multipartBody("memo.txt", textBytes((int) MAX_FILE_SIZE));
            var exactLimit = upload(client, base, csrfHeader, csrf, exactLimitBody);
            assertThat(exactLimit.statusCode()).isEqualTo(200);
            assertThat(exactLimit.body()).contains("memo.txt").contains("1048576 bytes");
            assertThat(temporaryFiles()).containsExactlyInAnyOrderElementsOf(temporaryFilesBeforeUpload);
            var freshPage = client.send(HttpRequest.newBuilder(URI.create(base)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(freshPage.statusCode()).isEqualTo(200);
            assertThat(freshPage.body()).doesNotContain("memo.txt");

            Set<Path> temporaryFilesBeforeOversizedUpload = temporaryFiles();
            var overLimitBody = multipartBody("large.txt", textBytes((int) MAX_FILE_SIZE + 1), csrf);
            var overLimit = upload(client, base, csrfHeader, csrf, overLimitBody, false, false);
            assertThat(overLimit.statusCode()).isEqualTo(413);
            assertThat(overLimit.body()).contains("送信内容が上限を超えています");
            assertThat(temporaryFiles()).containsExactlyInAnyOrderElementsOf(temporaryFilesBeforeOversizedUpload);
        }
    }

    private Set<Path> temporaryFiles() throws Exception {
        var tempDirectory = (java.io.File) application.getServletContext()
                .getAttribute(ServletContext.TEMPDIR);
        try (var entries = Files.list(tempDirectory.toPath())) {
            return entries.map(Path::getFileName).collect(Collectors.toSet());
        }
    }

    private static String metaContent(String html, String name) {
        Matcher matcher = Pattern.compile("<meta name=\"" + Pattern.quote(name)
                + "\" content=\"([^\"]+)\"").matcher(html);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private static byte[] multipartBody(String fileName, byte[] contents) throws Exception {
        return multipartBody(fileName, contents, null);
    }

    private static byte[] multipartBody(String fileName, byte[] contents, String csrf) throws Exception {
        String boundary = "----FileUploadTestBoundary";
        var body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"files\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(contents);
        body.write("\r\n".getBytes(StandardCharsets.UTF_8));
        if (csrf != null) {
            body.write(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"_csrf\"\r\n\r\n" + csrf + "\r\n")
                    .getBytes(StandardCharsets.UTF_8));
        }
        body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return body.toByteArray();
    }

    private static byte[] textBytes(int size) {
        var contents = new byte[size];
        Arrays.fill(contents, (byte) 'a');
        return contents;
    }

    private static HttpResponse<String> upload(HttpClient client, String url, String csrfHeader, String csrf,
                                                byte[] body) throws Exception {
        return upload(client, url, csrfHeader, csrf, body, true);
    }

    private static HttpResponse<String> upload(HttpClient client, String url, String csrfHeader, String csrf,
                                                byte[] body, boolean htmx) throws Exception {
        return upload(client, url, csrfHeader, csrf, body, htmx, true);
    }

    private static HttpResponse<String> upload(HttpClient client, String url, String csrfHeader, String csrf,
                                                byte[] body, boolean htmx, boolean includeCsrfHeader)
            throws Exception {
        var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=----FileUploadTestBoundary");
        if (!htmx) {
            request.header("Accept", "text/html");
        }
        if (includeCsrfHeader) {
            request.header(csrfHeader, csrf);
        }
        request.POST(HttpRequest.BodyPublishers.ofByteArray(body));
        if (htmx) {
            request.header("HX-Request", "true");
        }
        return client.send(request.build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
