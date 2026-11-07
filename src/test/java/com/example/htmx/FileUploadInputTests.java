package com.example.htmx;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FileUploadInputTests {
    private static final String BASE = "/examples/file-upload-input";
    private static final String FORM_ID = "file-upload-input-form";
    private static final long MAX_FILE_SIZE = 1024L * 1024L;

    @Autowired MockMvc mvc;

    @Test
    void pageExplainsFilePreservationAndHasStablePreservedInput() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("hx-preserve")))
                .andExpect(content().string(containsString("id=\"upload-input-file\"")))
                .andExpect(content().string(containsString("hx-target=\"#" + FORM_ID + "\"")))
                .andExpect(content().string(containsString("hx-swap=\"outerHTML\"")))
                .andExpect(content().string(containsString("通常フォーム")))
                .andExpect(content().string(containsString("aria-current=\"page\"")));
    }

    @Test
    void validHtmxSubmissionReturnsSuccessWithoutFileContents() throws Exception {
        String secret = "表示してはいけない本文";
        var file = textFile("memo.txt", secret.getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart(BASE).file(file).param("title", "月次資料")
                        .with(csrf()).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("data-upload-input-success")))
                .andExpect(content().string(containsString("月次資料")))
                .andExpect(content().string(containsString("memo.txt")))
                .andExpect(content().string(containsString("hx-preserve")))
                .andExpect(content().string(not(containsString(secret))));
    }

    @Test
    void blankTitleReturns422WithPreservedFileAndError() throws Exception {
        var file = textFile("memo.txt", "内容".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart(BASE).file(file).param("title", "").with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("id=\"" + FORM_ID + "\"")))
                .andExpect(content().string(containsString("id=\"upload-input-file\"")))
                .andExpect(content().string(containsString("hx-preserve")))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")))
                .andExpect(content().string(containsString("タイトルを入力してください")));
    }

    @Test
    void overlongTitleReturns422AndRetainsSubmittedTitle() throws Exception {
        var file = textFile("memo.txt", "text".getBytes(StandardCharsets.UTF_8));
        String title = "あ".repeat(61);

        mvc.perform(multipart(BASE).file(file).param("title", title).with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("タイトルは60文字以内")))
                .andExpect(content().string(containsString("value=\"" + title + "\"")))
                .andExpect(content().string(containsString("hx-preserve")));
    }

    @Test
    void rejectsUnsupportedExtensionAndPreservesSelectedInput() throws Exception {
        var file = new MockMultipartFile("files", "memo.bin", "text/plain",
                "plain text".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart(BASE).file(file).param("title", "資料").with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("拡張子が .txt")))
                .andExpect(content().string(containsString("id=\"upload-input-file\"")))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")))
                .andExpect(content().string(containsString("hx-preserve")));
    }

    @Test
    void rejectsMalformedUtf8AndDoesNotReturnFileContent() throws Exception {
        var file = new MockMultipartFile("files", "memo.txt", "text/plain", new byte[]{(byte) 0xc3, (byte) 0x28});

        mvc.perform(multipart(BASE).file(file).param("title", "資料").with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("UTF-8 のテキストではありません")))
                .andExpect(content().string(containsString("hx-preserve")));
    }

    @Test
    void missingAndMultipleFilesReturn422() throws Exception {
        mvc.perform(multipart(BASE).param("title", "資料").with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("1件選択してください")));

        var first = textFile("first.txt", "one".getBytes(StandardCharsets.UTF_8));
        var other = new MockMultipartFile("other", "second.txt", "text/plain",
                "two".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart(BASE).file(first).file(other).param("title", "資料")
                        .with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("1件選択してください")));
    }

    @Test
    void oversizedFileReturns413() throws Exception {
        var file = new MockMultipartFile("files", "large.txt", "text/plain",
                new byte[(int) MAX_FILE_SIZE + 1]);

        mvc.perform(multipart(BASE).file(file).param("title", "資料").with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(413))
                .andExpect(content().string(containsString("送信内容が上限を超えています")));
    }

    @Test
    void normalFormValidationReturnsFullPageAndRetainsOtherInput() throws Exception {
        var file = textFile("memo.txt", "text".getBytes(StandardCharsets.UTF_8));
        String title = "<".repeat(61);

        mvc.perform(multipart(BASE).file(file).param("title", title).with(csrf()))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("value=\"" + "&lt;".repeat(61) + "\"")))
                .andExpect(content().string(containsString("hx-preserve")));
    }

    @Test
    void successfulNormalFormRedirectsWithFlashNotice() throws Exception {
        var file = textFile("memo.txt", "text".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart(BASE).file(file).param("title", "月次資料").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("uploadNotice", containsString("月次資料")));
    }

    @Test
    void uploadRequiresCsrf() throws Exception {
        var file = textFile("memo.txt", "text".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart(BASE).file(file).param("title", "月次資料").header("HX-Request", "true"))
                .andExpect(status().isForbidden());
    }

    private static MockMultipartFile textFile(String name, byte[] contents) {
        return new MockMultipartFile("files", name, "text/plain", contents);
    }
}
