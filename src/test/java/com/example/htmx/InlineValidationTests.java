package com.example.htmx;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** メール単独検証と全体の再検証、CSRF、断片・通常ページ、入力の寿命を確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class InlineValidationTests {
    private static final String BASE = "/examples/inline-validation";
    @Autowired MockMvc mvc;

    @Test
    void pageExposesSafeConfigurationFieldValidationAndPlainForm() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("hx-trigger=\"change\"")))
                .andExpect(content().string(containsString("hx-params=\"email\"")))
                .andExpect(content().string(containsString("hx-sync=\"closest form:abort\"")))
                .andExpect(content().string(containsString("hx-post=\"" + BASE + "/email\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("aria-describedby=\"email-feedback\"")))
                .andExpect(content().string(not(containsString("data-email-valid=\"true\""))));
    }

    @Test
    void emailOnlyIgnoresNameAndReturnsFeedbackRatherThanAnInput() throws Exception {
        mvc.perform(post(BASE + "/email").with(csrf().asHeader()).header("HX-Request", "true")
                        .param("email", "sample@example.com").param("name", ""))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("data-email-valid=\"true\"")))
                .andExpect(content().string(not(containsString("<input"))))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("名前を入力してください。"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "invalid", "a@invalid@"})
    void invalidEmailIs422ForBothValidationRoutes(String email) throws Exception {
        for (String suffix : new String[]{"/email", ""}) {
            mvc.perform(post(BASE + suffix).with(csrf()).header("HX-Request", "true")
                            .param("email", email).param("name", "架空の名前"))
                    .andExpect(status().is(422))
                    .andExpect(content().string(containsString("data-email-valid=\"false\"")))
                    .andExpect(content().string(not(containsString("全項目の検証に成功しました。"))));
        }
    }

    @ParameterizedTest
    @CsvSource({"253,200", "254,200", "255,422"})
    void emailLengthBoundariesAreSharedByFieldAndSubmission(int length, int expected) throws Exception {
        String domain = "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(60) + ".com";
        String email = "a".repeat(length - domain.length() - 1) + "@" + domain;
        for (String suffix : new String[]{"/email", ""}) {
            mvc.perform(post(BASE + suffix).with(csrf()).header("HX-Request", "true")
                            .param("email", email).param("name", "架空の名前"))
                    .andExpect(status().is(expected));
        }
    }

    @ParameterizedTest
    @CsvSource({"59,200", "60,200", "61,422"})
    void nameLengthBoundariesAreCheckedOnSubmission(int length, int expected) throws Exception {
        mvc.perform(post(BASE).with(csrf()).header("HX-Request", "true")
                        .param("email", "sample@example.com").param("name", "名".repeat(length)))
                .andExpect(status().is(expected));
    }

    @Test
    void submissionRevalidatesWithoutTrustingPreviousFieldChecks() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(post(BASE + "/email").session(session).with(csrf())
                        .header("HX-Request", "true").param("email", "sample@example.com"))
                .andExpect(status().isOk());
        mvc.perform(post(BASE).session(session).with(csrf()).header("HX-Request", "true")
                        .param("email", "invalid").param("name", ""))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("id=\"validation-panel\"")))
                .andExpect(content().string(containsString("value=\"invalid\"")))
                .andExpect(content().string(containsString("名前を入力してください。")))
                .andExpect(content().string(containsString("メールアドレスの形式を確認してください。")))
                .andExpect(content().string(not(containsString("<html"))));
        mvc.perform(post(BASE).session(session).with(csrf()).header("HX-Request", "true")
                        .param("email", "sample@example.com").param("name", "架空の名前"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("全項目の検証に成功しました。")))
                .andExpect(content().string(containsString("登録やメール送信は行っていません。")));
        mvc.perform(get(BASE).session(session))
                .andExpect(content().string(not(containsString("value=\"架空の名前\""))))
                .andExpect(content().string(not(containsString("value=\"sample@example.com\""))));
        mvc.perform(get(BASE).session(new MockHttpSession()))
                .andExpect(content().string(not(containsString("value=\"架空の名前\""))));
    }

    @Test
    void missingEmailIsRejectedWithoutAnyPriorFieldValidation() throws Exception {
        mvc.perform(post(BASE + "/email").with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("メールアドレスを入力してください。")));
        mvc.perform(post(BASE).with(csrf()).header("HX-Request", "true").param("name", "架空"))
                .andExpect(status().is(422));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/email"})
    void allPostRoutesEnforceCsrfWithAndWithoutHxRequest(String suffix) throws Exception {
        var session = new MockHttpSession();
        for (String hx : new String[]{"true", "false"}) {
            mvc.perform(post(BASE + suffix).session(session).header("HX-Request", hx)
                            .param("name", "架空").param("email", "sample@example.com"))
                    .andExpect(status().isForbidden());
            mvc.perform(post(BASE + suffix).session(session).with(csrf().useInvalidToken())
                            .header("HX-Request", hx).param("name", "架空").param("email", "sample@example.com"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void plainFormsUseRenderedTokenAndRenderFullPagesForSuccessAndErrors() throws Exception {
        var session = new MockHttpSession();
        String page = mvc.perform(get(BASE).session(session)).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        var tokenMatcher = Pattern.compile("<input[^>]*name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(page);
        assertThat(tokenMatcher.find()).isTrue();
        String token = tokenMatcher.group(1);
        for (String suffix : new String[]{"", "/email"}) {
            mvc.perform(post(BASE + suffix).session(session).param("_csrf", token)
                            .param("name", "架空").param("email", "sample@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("<html lang=\"ja\"")));
            mvc.perform(post(BASE + suffix).session(session).param("_csrf", token)
                            .param("name", "架空").param("email", "invalid"))
                    .andExpect(status().is(422))
                    .andExpect(content().string(containsString("<html lang=\"ja\"")))
                    .andExpect(content().string(containsString("value=\"invalid\"")));
        }
    }

    @Test
    void userValuesAreEscapedOnBothSuccessAndInvalidFullForm() throws Exception {
        for (String email : new String[]{"sample@example.com", "<script>bad</script>"}) {
            mvc.perform(post(BASE).with(csrf()).header("HX-Request", "true")
                            .param("name", "<script>alert(1)</script>").param("email", email))
                    .andExpect(status().is(email.contains("<") ? 422 : 200))
                    .andExpect(content().string(containsString("&lt;script&gt;")))
                    .andExpect(content().string(not(containsString("<script>"))));
        }
    }
}
