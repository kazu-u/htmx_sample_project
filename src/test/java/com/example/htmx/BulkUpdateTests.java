package com.example.htmx;

import com.example.htmx.examples.bulkupdate.BulkUpdateState;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Security・Thymeleafを通して全行保存、入力拒否、セッション分離、競合を確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class BulkUpdateTests {
    private static final String BASE = "/examples/bulk-update";
    @Autowired MockMvc mvc;

    @Test
    void pageAndFragmentExposeBoundedCheckboxesAndSafeConfiguration() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(get(BASE).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("hx-sync=\"this:drop\"")))
                .andExpect(content().string(containsString("\"allowEval\":false")))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")));
        var before = snapshot(session);
        mvc.perform(get(BASE).session(session).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"bulk-panel\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("<script"))))
                .andExpect(header().string("Vary", containsString("HX-Request")));
        assertThat(snapshot(session)).isEqualTo(before);
    }

    @Test
    void savesAllRowsAndIsolatesSessions() throws Exception {
        var session = new MockHttpSession();
        var before = snapshot(session);
        mvc.perform(post(BASE).session(session).with(csrf().asHeader()).header("HX-Request", "true")
                        .param("revision", before.revision()).param("activeIds", "2", "4"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("全4行の有効・無効を保存しました。")))
                .andExpect(content().string(not(containsString("<html"))));
        assertThat(snapshot(session).people().stream().map(BulkUpdateState.Person::active).toList())
                .containsExactly(false, true, false, true);
        assertThat(snapshot(session).revision()).isNotEqualTo(before.revision());
        assertThat(snapshot(new MockHttpSession()).people().stream().map(BulkUpdateState.Person::active).toList())
                .containsExactly(true, false, true, false);
    }

    @Test
    void returnedRevisionAllowsConsecutiveSavesIncludingAfter422() throws Exception {
        var session = new MockHttpSession();
        String revision = snapshot(session).revision();
        mvc.perform(post(BASE).session(session).with(csrf()).header("HX-Request", "true")
                        .param("revision", revision).param("activeIds", "bad"))
                .andExpect(status().is(422));
        for (int attempt = 0; attempt < 3; attempt++) {
            var result = mvc.perform(post(BASE).session(session).with(csrf()).header("HX-Request", "true")
                            .param("revision", revision).param("activeIds", "2"))
                    .andExpect(status().isOk()).andReturn();
            var matcher = java.util.regex.Pattern.compile("<input[^>]*name=\"revision\"[^>]*value=\"([^\"]+)\"")
                    .matcher(result.getResponse().getContentAsString());
            assertThat(matcher.find()).isTrue();
            revision = matcher.group(1);
            assertThat(revision).isEqualTo(snapshot(session).revision());
        }
    }

    @Test
    void allCheckedAndAllUncheckedAreValidAndNormalFormsRedirect() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(post(BASE).session(session).with(csrf()).param("revision", snapshot(session).revision())
                        .param("activeIds", "1", "2", "3", "4"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(BASE));
        assertThat(snapshot(session).people()).allMatch(BulkUpdateState.Person::active);
        mvc.perform(post(BASE).session(session).with(csrf()).param("revision", snapshot(session).revision()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(BASE));
        assertThat(snapshot(session).people()).noneMatch(BulkUpdateState.Person::active);
        mvc.perform(post(BASE + "/reset").session(session).with(csrf())
                        .param("revision", snapshot(session).revision()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(BASE));
        assertThat(snapshot(session).people().stream().map(BulkUpdateState.Person::active).toList())
                .containsExactly(true, false, true, false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "5", "-1", "abc", "", "<script>alert(1)</script>"})
    void unknownAndMalformedIdsRejectTheWholeUpdate(String invalid) throws Exception {
        var session = new MockHttpSession();
        var before = snapshot(session);
        mvc.perform(post(BASE).session(session).with(csrf()).header("HX-Request", "true")
                        .param("revision", before.revision()).param("activeIds", "2", invalid))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("保存は行っていません。")))
                .andExpect(content().string(matchesPattern("(?s).*<input[^>]*name=\"activeIds\"[^>]*value=\"2\"[^>]*checked=\"checked\"[^>]*>.*")))
                .andExpect(content().string(not(containsString("<script>alert"))));
        assertThat(snapshot(session)).isEqualTo(before);
    }

    @Test
    void duplicateAndOversizedSelectionsAreRejectedAndNormalErrorsReturnFullPage() throws Exception {
        var session = new MockHttpSession();
        var before = snapshot(session);
        for (String[] ids : new String[][] {{"2", "2"}, {"1", "2", "3", "4", "1"}}) {
            mvc.perform(post(BASE).session(session).with(csrf()).param("revision", before.revision())
                            .param("activeIds", ids))
                    .andExpect(status().is(422))
                    .andExpect(content().string(containsString("<html lang=\"ja\"")));
            assertThat(snapshot(session)).isEqualTo(before);
        }

    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1,2", " ", "2,"})
    void singleRawValueCannotBeConvertedIntoAnUncheckedOrMultipleSelection(String invalid) throws Exception {
        var session = new MockHttpSession();
        var before = snapshot(session);
        mvc.perform(post(BASE).session(session).with(csrf()).header("HX-Request", "true")
                        .param("revision", before.revision()).param("activeIds", invalid))
                .andExpect(status().is(422));
        assertThat(snapshot(session)).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "short", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"})
    void invalidRevisionReturns422WithoutMutation(String revision) throws Exception {
        var session = new MockHttpSession();
        var before = snapshot(session);
        mvc.perform(post(BASE).session(session).with(csrf()).param("revision", revision))
                .andExpect(status().is(422)).andExpect(content().string(containsString("版情報")));
        assertThat(snapshot(session)).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"activeIds[]", "!activeIds", "_activeIds", "activeIds[0]"})
    void parameterAliasesCannotBypassRawValueValidation(String name) throws Exception {
        var session = new MockHttpSession();
        var before = snapshot(session);
        mvc.perform(post(BASE).session(session).with(csrf()).header("HX-Request", "true")
                        .param("revision", before.revision()).param(name, "1,2"))
                .andExpect(status().is(422));
        assertThat(snapshot(session)).isEqualTo(before);
    }

    @Test
    void staleSaveAndResetCannotOverwriteChangesEvenWithInvalidIds() throws Exception {
        var session = new MockHttpSession();
        var before = snapshot(session);
        mvc.perform(post(BASE).session(session).with(csrf()).param("revision", before.revision()))
                .andExpect(status().is3xxRedirection());
        var after = snapshot(session);
        mvc.perform(post(BASE).session(session).with(csrf()).param("revision", before.revision())
                        .param("activeIds", "bad"))
                .andExpect(status().isConflict());
        mvc.perform(post(BASE + "/reset").session(session).with(csrf()).param("revision", before.revision()))
                .andExpect(status().isConflict());
        assertThat(snapshot(session)).isEqualTo(after);
    }

    @Test
    void csrfIsRequiredForBothUpdateEndpoints() throws Exception {
        var session = new MockHttpSession();
        var before = snapshot(session);
        for (String path : new String[] {BASE, BASE + "/reset"}) {
            mvc.perform(post(path).session(session).param("revision", before.revision()))
                    .andExpect(status().isForbidden());
            mvc.perform(post(path).session(session).with(csrf().useInvalidToken())
                            .header("HX-Request", "true").param("revision", before.revision()))
                    .andExpect(status().isForbidden());
        }
        assertThat(snapshot(session)).isEqualTo(before);
    }

    @Test
    void concurrentSavesOfTheSameRevisionHaveOneWinner() throws Exception {
        var state = new BulkUpdateState();
        String revision = state.snapshot().revision();
        Callable<Integer> save = () -> {
            try {
                state.save(Set.of("2"), revision);
                return 200;
            } catch (ResponseStatusException exception) {
                return exception.getStatusCode().value();
            }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(save);
            var second = executor.submit(save);
            assertThat(java.util.List.of(first.get(), second.get())).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(state.snapshot().people().stream().map(BulkUpdateState.Person::active).toList())
                .containsExactly(false, true, false, false);
    }

    private BulkUpdateState.Snapshot snapshot(MockHttpSession session) throws Exception {
        mvc.perform(get(BASE).session(session)).andExpect(status().isOk());
        return ((BulkUpdateState) session.getAttribute("scopedTarget.bulkUpdateState")).snapshot();
    }
}
