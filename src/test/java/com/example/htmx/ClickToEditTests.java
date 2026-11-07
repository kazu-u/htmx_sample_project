package com.example.htmx;

import com.example.htmx.examples.clicktoedit.ContactForm;
import com.example.htmx.examples.clicktoedit.ContactState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 実際の Security・Controller・Thymeleaf を通し、断片だけでなく入力と状態も検証する。 */
@SpringBootTest
@AutoConfigureMockMvc
class ClickToEditTests {
    private static final String BASE = "/examples/click-to-edit";
    @Autowired MockMvc mvc;

    @Test
    void fullPageAndLocalAssetsArePublicAndProtectedByHeaders() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("aria-label=\"サンプル\"")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("クリックして続きを読み込む")))
                .andExpect(content().string(containsString("山田 花子")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("allowEval")))
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
        mvc.perform(get("/vendor/htmx/htmx-2.0.11.min.js")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")));
        mvc.perform(get("/css/app.css")).andExpect(status().isOk());
        mvc.perform(get("/js/app.js")).andExpect(status().isOk());
        mvc.perform(get("/")).andExpect(redirectedUrl(BASE));
        mvc.perform(get("/login")).andExpect(status().isNotFound());
    }

    @Test
    void editAndCancelReturnOnlyContactWithoutScripts() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(get(BASE + "/contact/edit").session(session).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"name\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("id=\"name-error\"")))
                .andExpect(content().string(containsString("id=\"email-error\"")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("<script"))));
        mvc.perform(get(BASE + "/contact").session(session).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("山田 花子")))
                .andExpect(content().string(not(containsString("name=\"name\""))));
    }

    @Test
    void savesWithHeaderTokenAndIsolatesSessions() throws Exception {
        var session = new MockHttpSession();
        var revision = revision(session);
        mvc.perform(post(BASE + "/contact").session(session).with(csrf().asHeader())
                        .header("HX-Request", "true").param("revision", revision)
                        .param("name", "佐藤 太郎").param("email", "taro@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("佐藤 太郎")))
                .andExpect(content().string(containsString("保存しました。")));
        mvc.perform(get(BASE).session(session)).andExpect(content().string(containsString("佐藤 太郎")));
        mvc.perform(get(BASE).session(new MockHttpSession()))
                .andExpect(content().string(containsString("山田 花子")))
                .andExpect(content().string(not(containsString("佐藤 太郎"))));
    }

    @Test
    void invalidInputPreservesValuesAndDoesNotSave() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(post(BASE + "/contact").session(session).with(csrf())
                        .header("HX-Request", "true").param("revision", revision(session))
                        .param("name", " ").param("email", "not-an-email"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("名前を入力してください。")))
                .andExpect(content().string(containsString("メールアドレスの形式を確認してください。")))
                .andExpect(content().string(containsString("value=\"not-an-email\"")))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")))
                .andExpect(content().string(not(containsString("<html"))));
        mvc.perform(get(BASE).session(session)).andExpect(content().string(containsString("山田 花子")));
    }

    @Test
    void staleRevisionTakesPrecedenceOverOtherValidationErrors() throws Exception {
        var session = new MockHttpSession();
        var staleRevision = revision(session);
        mvc.perform(post(BASE + "/contact").session(session).with(csrf())
                        .param("revision", revision(session)).param("name", "先行更新")
                        .param("email", "new@example.com"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post(BASE + "/contact").session(session).with(csrf()).header("HX-Request", "true")
                        .param("revision", staleRevision).param("name", " ").param("email", "invalid"))
                .andExpect(status().isConflict());
        mvc.perform(get(BASE).session(session))
                .andExpect(content().string(containsString("先行更新")))
                .andExpect(content().string(not(containsString("invalid"))));
    }

    @Test
    void revisionValidationBoundariesMatchTheDocumentedContract() throws Exception {
        var session = new MockHttpSession();
        var original = mvc.perform(get(BASE).session(session)).andReturn();
        var originalContact = (ContactState.Contact) original.getModelAndView().getModel().get("contact");
        String[] revisions = {"x".repeat(36), "x".repeat(37), " "};
        int[] expectedStatuses = {409, 422, 422};

        for (int index = 0; index < revisions.length; index++) {
            var result = mvc.perform(post(BASE + "/contact").session(session).with(csrf())
                            .header("HX-Request", "true").param("revision", revisions[index])
                            .param("name", "変更しない名前").param("email", "valid@example.com"))
                    .andExpect(status().is(expectedStatuses[index]))
                    .andReturn();
            if (expectedStatuses[index] == 422) {
                var response = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
                assertThat(response).contains("id=\"revision-error\"")
                        .contains("tabindex=\"-1\"").contains("role=\"alert\"");
            }
            mvc.perform(get(BASE).session(session))
                    .andExpect(content().string(containsString(originalContact.name())));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 59, 60, 61})
    void nameBoundaries(int length) throws Exception {
        var session = new MockHttpSession();
        mvc.perform(post(BASE + "/contact").session(session).with(csrf()).header("HX-Request", "true")
                        .param("revision", revision(session)).param("name", "あ".repeat(length))
                        .param("email", "valid@example.com"))
                .andExpect(status().is(length == 0 || length > 60 ? 422 : 200));
    }

    @ParameterizedTest
    @ValueSource(ints = {253, 254, 255})
    void emailLengthBoundaries(int length) throws Exception {
        // 各ラベル・ローカル部の長さも妥当なメールで、総文字数の上限だけを変える。
        String domain = "a".repeat(63) + "." + "b".repeat(63) + "." + "c".repeat(60) + ".com";
        String email = "x".repeat(length - domain.length() - 1) + "@" + domain;
        var session = new MockHttpSession();
        mvc.perform(post(BASE + "/contact").session(session).with(csrf()).header("HX-Request", "true")
                        .param("revision", revision(session)).param("name", "有効な名前").param("email", email))
                .andExpect(status().is(length > 254 ? 422 : 200));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "bad", "a@invalid..example"})
    void rejectsInvalidEmails(String email) throws Exception {
        var session = new MockHttpSession();
        mvc.perform(post(BASE + "/contact").session(session).with(csrf()).param("revision", revision(session))
                        .param("name", "名前").param("email", email))
                .andExpect(status().is(422)).andExpect(content().string(containsString("<html")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/contact", "/reset"})
    void csrfRejectsMissingAndInvalidTokensEvenWithHxHeader(String path) throws Exception {
        var session = new MockHttpSession();
        var revision = revision(session);
        mvc.perform(post(BASE + path).session(session).header("HX-Request", "true")
                        .param("revision", revision).param("name", "変更").param("email", "x@example.com"))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + path).session(session).with(csrf().useInvalidToken())
                        .header("HX-Request", "true").param("revision", revision)
                        .param("name", "変更").param("email", "x@example.com"))
                .andExpect(status().isForbidden());
        mvc.perform(get(BASE).session(session)).andExpect(content().string(containsString("山田 花子")));
    }

    @Test
    void normalFormsRedirectAndResetInvalidatesOldForm() throws Exception {
        var session = new MockHttpSession();
        var oldRevision = revision(session);
        mvc.perform(post(BASE + "/contact").session(session).with(csrf()).param("revision", oldRevision)
                        .param("name", "別の名前").param("email", "new@example.com"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("notice", "保存しました。"));
        var currentRevision = revision(session);
        mvc.perform(post(BASE + "/reset").session(session).with(csrf().asHeader())
                        .header("HX-Request", "true").param("revision", currentRevision))
                .andExpect(status().isOk()).andExpect(content().string(containsString("山田 花子")));
        mvc.perform(post(BASE + "/contact").session(session).with(csrf()).param("revision", oldRevision)
                        .param("name", "上書き").param("email", "stale@example.com"))
                .andExpect(status().isConflict());
        mvc.perform(post(BASE + "/reset").session(session).with(csrf()).param("revision", currentRevision))
                .andExpect(status().isConflict());
        mvc.perform(get(BASE + "/reset").session(session)).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void normalResetRedirectsAndMissingRevisionIsRejected() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(post(BASE + "/reset").session(session).with(csrf()).param("revision", revision(session)))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("notice", "初期状態に戻しました。"));
        mvc.perform(post(BASE + "/reset").session(session).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(post(BASE + "/contact").session(session).with(csrf())
                        .param("name", "名前").param("email", "a@example.com"))
                .andExpect(status().is(422));
    }

    @Test
    void normalFormsSubmitTheirActuallyRenderedCsrfTokens() throws Exception {
        var session = new MockHttpSession();
        var edit = mvc.perform(get(BASE + "/contact/edit").session(session))
                .andExpect(status().isOk()).andReturn();
        var form = (ContactForm) edit.getModelAndView().getModel().get("contactForm");
        mvc.perform(post(BASE + "/contact").session(session)
                        .param("_csrf", renderedToken(edit)).param("revision", form.getRevision())
                        .param("name", "通常フォームの保存").param("email", "form@example.com"))
                .andExpect(status().is3xxRedirection());

        var display = mvc.perform(get(BASE).session(session))
                .andExpect(content().string(containsString("通常フォームの保存"))).andReturn();
        var contact = (ContactState.Contact) display.getModelAndView().getModel().get("contact");
        mvc.perform(post(BASE + "/reset").session(session)
                        .param("_csrf", renderedToken(display)).param("revision", contact.revision()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get(BASE).session(session)).andExpect(content().string(containsString("山田 花子")));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "403,セッション失効や CSRF トークン不一致の可能性があります",
            "409,別の操作で更新されています",
            "404,操作対象や入力を確認してください"
    })
    void errorPageShowsRecoveryGuidance(int status, String guidance) throws Exception {
        mvc.perform(get("/error")
                        .accept(org.springframework.http.MediaType.TEXT_HTML)
                        .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE, status)
                        .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_REQUEST_URI, BASE))
                .andExpect(status().is(status))
                .andExpect(content().string(containsString("HTTP <span>" + status + "</span>")))
                .andExpect(content().string(containsString(guidance)));
    }

    private String renderedToken(MvcResult result) throws Exception {
        var matcher = java.util.regex.Pattern.compile(
                "<input[^>]*name=\"_csrf\"[^>]*value=\"([^\"]+)\"")
                .matcher(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(matcher.find()).as("実際のフォームに CSRF hidden 項目が含まれる").isTrue();
        return matcher.group(1);
    }

    @Test
    void htmxSafetySettingsMatchTheRenderedConfiguration() throws Exception {
        var result = mvc.perform(get(BASE)).andExpect(status().isOk()).andReturn();
        var matcher = java.util.regex.Pattern.compile("<meta name=\"htmx-config\" content='([^']+)'")
                .matcher(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(matcher.find()).isTrue();
        var config = tools.jackson.databind.json.JsonMapper.builder().build().readTree(matcher.group(1));
        assertThat(config.get("selfRequestsOnly").asBoolean()).isTrue();
        assertThat(config.get("allowScriptTags").asBoolean()).isFalse();
        assertThat(config.get("allowEval").asBoolean()).isFalse();
        assertThat(config.get("includeIndicatorStyles").asBoolean()).isFalse();
        assertThat(config.get("timeout").asInt()).isEqualTo(10000);
        assertThat(config.get("historyCacheSize").asInt()).isZero();
    }

    @Test
    void escapesUserInputInDisplayAndEdit() throws Exception {
        var session = new MockHttpSession();
        var input = "<script>alert(1)</script>";
        mvc.perform(post(BASE + "/contact").session(session).with(csrf()).header("HX-Request", "true")
                        .param("revision", revision(session)).param("name", input).param("email", "x@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString(input))));
        mvc.perform(get(BASE + "/contact/edit").session(session).header("HX-Request", "true"))
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString(input))));
    }

    @Test
    void stateUsesAtomicRevisionCheckAndResetCreatesNewRevision() {
        var state = new ContactState();
        var before = state.snapshot();
        var form = new ContactForm();
        form.setName("更新");
        form.setEmail("new@example.com");
        form.setRevision(before.revision());
        state.save(form);
        assertThat(state.snapshot().revision()).isNotEqualTo(before.revision());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> state.save(form))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        state.reset(state.snapshot().revision());
        assertThat(state.snapshot().name()).isEqualTo(before.name());
        assertThat(state.snapshot().revision()).isNotEqualTo(before.revision());
    }

    @Test
    void simultaneousUpdatesOnlyAcceptOneRevision() throws Exception {
        var state = new ContactState();
        var form = new ContactForm();
        form.setName("同時更新");
        form.setEmail("parallel@example.com");
        form.setRevision(state.snapshot().revision());
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> update = () -> {
                ready.countDown();
                assertThat(start.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                try {
                    state.save(form);
                    return true;
                } catch (org.springframework.web.server.ResponseStatusException conflict) {
                    assertThat(conflict.getStatusCode().value()).isEqualTo(409);
                    return false;
                }
            };
            var first = executor.submit(update);
            var second = executor.submit(update);
            assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(java.util.List.of(first.get(5, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(5, java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        }
    }

    private String revision(MockHttpSession session) throws Exception {
        MvcResult result = mvc.perform(get(BASE + "/contact/edit").session(session))
                .andExpect(status().isOk()).andReturn();
        return ((ContactForm) result.getModelAndView().getModel().get("contactForm")).getRevision();
    }
}
