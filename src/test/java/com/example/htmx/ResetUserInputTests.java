package com.example.htmx;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ResetUserInputTests {
    private static final String BASE = "/examples/reset-user-input";

    @Autowired MockMvc mvc;

    @Test
    void pageProvidesAPlainFormAndExternalResetHandler() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("hx-target=\"#note-list\"")))
                .andExpect(content().string(containsString("hx-swap=\"afterbegin\"")))
                .andExpect(content().string(containsString("/js/reset-user-input.js")))
                .andExpect(content().string(not(containsString("hx-on::after-request=\""))))
                .andExpect(content().string(containsString("aria-current=\"page\"")));
    }

    @Test
    void validHtmxSubmissionReturnsEscapedNoteFragment() throws Exception {
        mvc.perform(post(BASE).param("text", "<script>alert(1)</script>")
                        .with(csrf()).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>"))))
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    void acceptsThe120CharacterBoundary() throws Exception {
        String text = "あ".repeat(120);

        mvc.perform(post(BASE).param("text", text).with(csrf()).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(text)));
    }

    @Test
    void blankHtmxSubmissionReturns422ErrorFragment() throws Exception {
        mvc.perform(post(BASE).param("text", "  ")
                        .with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("id=\"note-error\"")))
                .andExpect(content().string(containsString("メモを入力してください。")))
                .andExpect(content().string(containsString("hx-swap-oob=\"outerHTML\"")))
                .andExpect(content().string(containsString("role=\"alert\"")))
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    void overlongHtmxSubmissionReturns422WithoutEchoingTheInvalidValue() throws Exception {
        mvc.perform(post(BASE).param("text", "あ".repeat(121))
                        .with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("メモは120文字以内")))
                .andExpect(content().string(not(containsString("あ".repeat(121)))));
    }

    @Test
    void successfulNormalFormReturnsFullPageWithAnEmptyForm() throws Exception {
        mvc.perform(post(BASE).param("text", "次のメモ").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("<li>次のメモ</li>")))
                .andExpect(content().string(containsString("<textarea id=\"note-text\"")))
                .andExpect(content().string(containsString("name=\"text\"></textarea>")));
    }

    @Test
    void invalidNormalFormReturnsFullPageWithEscapedValueAndError() throws Exception {
        mvc.perform(post(BASE).param("text", "<".repeat(121)).with(csrf()))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("&lt;".repeat(121))))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")))
                .andExpect(content().string(containsString("メモは120文字以内")));
    }

    @Test
    void postRequiresCsrf() throws Exception {
        mvc.perform(post(BASE).param("text", "CSRFなし"))
                .andExpect(status().isForbidden());
    }

    @Test
    void submittedNotesAreNotPersistedAcrossPageLoads() throws Exception {
        mvc.perform(post(BASE).param("text", "一時的なメモ").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("一時的なメモ")));

        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("一時的なメモ"))));
    }
}
