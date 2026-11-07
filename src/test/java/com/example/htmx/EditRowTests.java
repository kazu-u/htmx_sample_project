package com.example.htmx;

import com.example.htmx.examples.editrow.EditRowState;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 実際のSecurity・Thymeleafを通し、行の独立性、入力、CSRF、版の競合を確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class EditRowTests {
    private static final String BASE = "/examples/edit-row";
    @Autowired MockMvc mvc;

    @Test
    void fullPageAndFragmentsUseRowTargetsAndAssociatedForms() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(get(BASE).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("id=\"person-row-3\"")))
                .andExpect(content().string(containsString("hx-sync=\"this:drop\"")))
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")));

        var before = snapshot(session);
        for (int id = 1; id <= 3; id++) {
            mvc.perform(get(BASE + "/" + id + "/edit").session(session).header("HX-Request", "true"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("id=\"person-row-" + id + "\"")))
                    .andExpect(content().string(containsString("id=\"row-" + id + "-name\"")))
                    .andExpect(content().string(containsString("form=\"edit-person-" + id + "\"")))
                    .andExpect(content().string(containsString("name=\"_csrf\"")))
                    .andExpect(content().string(containsString("hx-params=\"none\"")))
                    .andExpect(content().string(not(containsString("<html"))))
                    .andExpect(content().string(not(containsString("<script"))));
        }
        mvc.perform(get(BASE + "/1").session(session).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("name=\"name\""))));
        assertThat(snapshot(session)).isEqualTo(before);
    }

    @Test
    void savingDifferentRowsIsIndependentAndSessionsAreIsolated() throws Exception {
        var session = new MockHttpSession();
        var before = snapshot(session);
        for (int id = 1; id <= 2; id++) {
            mvc.perform(post(BASE + "/" + id).session(session).with(csrf().asHeader())
                            .header("HX-Request", "true").param("revision", before.people().get(id - 1).revision())
                            .param("name", "変更" + id).param("email", "changed" + id + "@example.com")
                            .param("id", "3"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("行を保存しました。")))
                    .andExpect(content().string(containsString("変更" + id)))
                    .andExpect(content().string(not(containsString("<html"))))
                    .andExpect(content().string(not(containsString("id=\"person-row-3\""))));
        }
        var after = snapshot(session);
        assertThat(after.people().get(0).name()).isEqualTo("変更1");
        assertThat(after.people().get(1).name()).isEqualTo("変更2");
        assertThat(after.people().get(2)).isEqualTo(before.people().get(2));
        assertThat(snapshot(new MockHttpSession()).people().getFirst().name()).isEqualTo("青空 花子");
    }

    @Test
    void invalidInputReturnsOnlyTheEditableRowAndDoesNotMutateState() throws Exception {
        var session = new MockHttpSession();
        var before = snapshot(session);
        mvc.perform(post(BASE + "/1").session(session).with(csrf())
                        .header("HX-Request", "true").param("revision", before.people().getFirst().revision())
                        .param("name", " ").param("email", "invalid"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("名前を入力してください。")))
                .andExpect(content().string(containsString("メールアドレスの形式を確認してください。")))
                .andExpect(content().string(containsString("value=\"invalid\"")))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")))
                .andExpect(content().string(containsString("id=\"row-1-email-error\"")))
                .andExpect(content().string(not(containsString("<html"))));
        assertThat(snapshot(session)).isEqualTo(before);
    }

    @ParameterizedTest
    @CsvSource({"59,253,200", "60,254,200", "61,254,422", "60,255,422"})
    void textLengthBoundariesAreEnforced(int nameLength, int emailLength, int expected) throws Exception {
        var session = new MockHttpSession();
        // 各メールラベルを63文字以下に保ち、全体の長さだけを変える。
        String domain = "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(60) + ".com";
        String email = "a".repeat(emailLength - domain.length() - 1) + "@" + domain;
        var before = snapshot(session);
        mvc.perform(post(BASE + "/1").session(session).with(csrf())
                        .header("HX-Request", "true").param("revision", before.people().getFirst().revision())
                        .param("name", "名".repeat(nameLength)).param("email", email))
                .andExpect(status().is(expected));
        if (expected == 200) {
            assertThat(snapshot(session).people().getFirst().email()).isEqualTo(email);
        } else {
            assertThat(snapshot(session)).isEqualTo(before);
        }
    }

    @ParameterizedTest
    @CsvSource({"0,422", "36,409", "37,422"})
    void revisionConstraintsAndConflictsHaveDistinctStatuses(int length, int expected) throws Exception {
        var session = new MockHttpSession();
        var before = snapshot(session);
        mvc.perform(post(BASE + "/1").session(session).with(csrf())
                        .header("HX-Request", "true").param("revision", "x".repeat(length))
                        .param("name", "保存しない").param("email", "valid@example.com"))
                .andExpect(status().is(expected));
        assertThat(snapshot(session)).isEqualTo(before);
    }

    @Test
    void staleRowOrResetCannotOverwriteNewerChangesEvenWhenInputIsInvalid() throws Exception {
        var session = new MockHttpSession();
        var before = snapshot(session);
        mvc.perform(post(BASE + "/1").session(session).with(csrf())
                        .param("revision", before.people().getFirst().revision())
                        .param("name", "先行更新").param("email", "first@example.com"))
                .andExpect(status().is3xxRedirection());
        var saved = snapshot(session);
        mvc.perform(post(BASE + "/1").session(session).with(csrf())
                        .param("revision", before.people().getFirst().revision())
                        .param("name", " ").param("email", "invalid"))
                .andExpect(status().isConflict());
        mvc.perform(post(BASE + "/reset").session(session).with(csrf()).param("revision", before.revision()))
                .andExpect(status().isConflict());
        assertThat(snapshot(session)).isEqualTo(saved);

        mvc.perform(post(BASE + "/reset").session(session).with(csrf()).param("revision", saved.revision()))
                .andExpect(redirectedUrl(BASE));
        var reset = snapshot(session);
        assertThat(reset.people().getFirst().name()).isEqualTo("青空 花子");
        assertThat(reset.people().getFirst().revision()).isNotEqualTo(before.people().getFirst().revision());
        mvc.perform(post(BASE + "/1").session(session).with(csrf())
                        .param("revision", saved.people().getFirst().revision())
                        .param("name", "古い保存").param("email", "stale@example.com"))
                .andExpect(status().isConflict());
        assertThat(snapshot(session)).isEqualTo(reset);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/1", "/reset"})
    void allUpdatesRejectMissingAndInvalidCsrfWithOrWithoutHxHeader(String path) throws Exception {
        var session = new MockHttpSession();
        var before = snapshot(session);
        for (String hx : new String[]{"true", "false"}) {
            mvc.perform(post(BASE + path).session(session).header("HX-Request", hx)
                            .param("revision", before.revision()).param("name", "不正更新")
                            .param("email", "valid@example.com"))
                    .andExpect(status().isForbidden());
            mvc.perform(post(BASE + path).session(session).with(csrf().useInvalidToken())
                            .header("HX-Request", hx).param("revision", before.revision())
                            .param("name", "不正更新").param("email", "valid@example.com"))
                    .andExpect(status().isForbidden());
        }
        assertThat(snapshot(session)).isEqualTo(before);
    }

    @Test
    void missingAndMalformedIdsAreRejectedForDisplayEditAndSave() throws Exception {
        var session = new MockHttpSession();
        var before = snapshot(session);
        for (String suffix : new String[]{"", "/edit"}) {
            mvc.perform(get(BASE + "/99" + suffix).session(session).header("HX-Request", "true"))
                    .andExpect(status().isNotFound());
            mvc.perform(get(BASE + "/invalid" + suffix).session(session))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post(BASE + "/99").session(session).with(csrf())
                        .param("revision", before.people().getFirst().revision())
                        .param("name", "対象なし").param("email", "valid@example.com"))
                .andExpect(status().isNotFound());
        mvc.perform(post(BASE + "/invalid").session(session).with(csrf()))
                .andExpect(status().isBadRequest());
        assertThat(snapshot(session)).isEqualTo(before);
    }

    @Test
    void plainFormsUseRenderedCsrfAndPreserveErrorsOnFullPages() throws Exception {
        var session = new MockHttpSession();
        var edit = mvc.perform(get(BASE + "/1/edit").session(session)).andExpect(status().isOk()).andReturn();
        String html = edit.getResponse().getContentAsString(StandardCharsets.UTF_8);
        var csrfMatcher = Pattern.compile("<input[^>]*name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertThat(csrfMatcher.find()).isTrue();
        String token = csrfMatcher.group(1);
        String revision = snapshot(session).people().getFirst().revision();
        mvc.perform(post(BASE + "/1").session(session).param("_csrf", token).param("revision", revision)
                        .param("name", "").param("email", "invalid"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("value=\"invalid\"")));
        mvc.perform(post(BASE + "/1").session(session).param("_csrf", token).param("revision", revision)
                        .param("name", "通常フォーム").param("email", "plain@example.com"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(BASE));
        assertThat(snapshot(session).people().getFirst().name()).isEqualTo("通常フォーム");
    }

    @Test
    void userValuesAreEscapedOnBothDisplayAndEditingRows() throws Exception {
        var session = new MockHttpSession();
        String name = "<script>alert(1)</script>";
        mvc.perform(post(BASE + "/1").session(session).with(csrf())
                        .param("revision", snapshot(session).people().getFirst().revision())
                        .param("name", name).param("email", "safe@example.com"))
                .andExpect(status().is3xxRedirection());
        for (String suffix : new String[]{"", "/edit"}) {
            mvc.perform(get(BASE + "/1" + suffix).session(session).header("HX-Request", "true"))
                    .andExpect(content().string(containsString("&lt;script&gt;")))
                    .andExpect(content().string(not(containsString(name))));
        }
    }

    @Test
    void concurrentSavesWithOneRevisionHaveExactlyOneWinner() throws Exception {
        var session = new MockHttpSession();
        String revision = snapshot(session).people().getFirst().revision();
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> save = () -> mvc.perform(post(BASE + "/1").session(session).with(csrf())
                            .header("HX-Request", "true").param("revision", revision)
                            .param("name", "競合テスト").param("email", "race@example.com"))
                    .andReturn().getResponse().getStatus();
            var results = executor.invokeAll(java.util.List.of(save, save));
            assertThat(java.util.List.of(results.get(0).get(), results.get(1).get()))
                    .containsExactlyInAnyOrder(200, 409);
        }
        assertThat(snapshot(session).people().getFirst().name()).isEqualTo("競合テスト");
    }

    private EditRowState.Snapshot snapshot(MockHttpSession session) throws Exception {
        MvcResult result = mvc.perform(get(BASE).session(session)).andExpect(status().isOk()).andReturn();
        var model = result.getModelAndView().getModel();
        @SuppressWarnings("unchecked")
        var people = (java.util.List<EditRowState.Person>) model.get("people");
        return new EditRowState.Snapshot(people, (String) model.get("listRevision"));
    }
}
