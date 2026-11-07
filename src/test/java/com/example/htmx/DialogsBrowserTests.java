package com.example.htmx;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriUtils;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** ブラウザーのダイアログ値、通常フォーム、CSRF、エスケープを確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class DialogsBrowserTests {
    private static final String BASE = "/examples/dialogs-browser";

    @Autowired
    MockMvc mvc;

    @Test
    void pageShowsBrowserDialogsAndAConfirmedNormalForm() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("hx-prompt=\"")))
                .andExpect(content().string(containsString("hx-confirm=\"")))
                .andExpect(content().string(containsString("hx-params=\"none\"")))
                .andExpect(content().string(containsString("/js/dialogs-browser.js")))
                .andExpect(content().string(containsString("name=\"confirmed\"")))
                .andExpect(content().string(containsString("id=\"confirmation-error\"")))
                .andExpect(content().string(not(containsString("aria-invalid=\"true\""))))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("aria-current=\"page\"")));
    }

    @Test
    void validHtmxPromptReturnsAnEscapedFragment() throws Exception {
        mvc.perform(post(BASE).with(csrf().asHeader())
                        .header("HX-Request", "true")
                        .header("HX-Prompt", encode("<script>alert(1)</script>")))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>"))))
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    void acceptsThe100CharacterPromptBoundary() throws Exception {
        String message = "あ".repeat(100);

        mvc.perform(post(BASE).with(csrf().asHeader())
                        .header("HX-Request", "true").header("HX-Prompt", encode(message)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(message)));
    }

    @Test
    void decodesJapaneseAndPreservesPlusAndPercentCharacters() throws Exception {
        String message = "日本語 + 50%";

        mvc.perform(post(BASE).with(csrf().asHeader()).header("HX-Request", "true")
                        .header("HX-Prompt", encode(message)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(message)));
    }

    @Test
    void emptyOrMissingPromptReturns422WithoutEchoingTheInput() throws Exception {
        mvc.perform(post(BASE).with(csrf().asHeader())
                        .header("HX-Request", "true").header("HX-Prompt", encode("  ")))
                .andExpect(status().is(422))
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("id=\"dialog-result\"")))
                .andExpect(content().string(containsString("role=\"alert\"")))
                .andExpect(content().string(containsString("入力が空です。")))
                .andExpect(content().string(not(containsString("  </p>"))));

        mvc.perform(post(BASE).with(csrf().asHeader()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("入力が空です。")));
    }

    @Test
    void overlongPromptReturns422WithoutEchoingTheInvalidValue() throws Exception {
        mvc.perform(post(BASE).with(csrf().asHeader()).header("HX-Request", "true")
                        .header("HX-Prompt", encode("a".repeat(101))))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("100文字以内")))
                .andExpect(content().string(not(containsString("a".repeat(101)))));
    }

    @Test
    void malformedEncodedPromptReturns422() throws Exception {
        mvc.perform(post(BASE).with(csrf().asHeader())
                        .header("HX-Request", "true").header("HX-Prompt", "%E0%A4%A"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("入力を読み取れませんでした。")));
    }

    @Test
    void normalFormReturnsTheSubmittedMessageInTheFullPage() throws Exception {
        mvc.perform(post(BASE).with(csrf())
                        .param("message", "<script>alert(1)</script>")
                        .param("confirmed", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert"))))
                .andExpect(content().string(not(containsString("checked"))));
    }

    @Test
    void normalFormPreservesInvalidMessageAndRequiresConfirmation() throws Exception {
        mvc.perform(post(BASE).with(csrf()).param("message", "送信前の値"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("value=\"送信前の値\"")))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")))
                .andExpect(content().string(not(containsString("checked"))))
                .andExpect(content().string(containsString("送信確認にチェックしてください。")));

        mvc.perform(post(BASE).with(csrf()).param("message", "　").param("confirmed", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("メッセージを入力してください。")))
                .andExpect(content().string(containsString("checked")));
    }

    @Test
    void htmxPostRequiresCsrf() throws Exception {
        mvc.perform(post(BASE).header("HX-Request", "true").header("HX-Prompt", "送信"))
                .andExpect(status().isForbidden());
    }

    private String encode(String message) {
        return UriUtils.encode(message, StandardCharsets.UTF_8);
    }
}
