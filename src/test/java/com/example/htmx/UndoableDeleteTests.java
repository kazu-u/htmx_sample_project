package com.example.htmx;

import com.example.htmx.examples.undoabledelete.UndoableDeleteState;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 期限付き削除、取り消し、CSRF、版競合、セッション分離を確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class UndoableDeleteTests {
    private static final String BASE = "/examples/undoable-delete";

    @Autowired
    MockMvc mvc;

    @Test
    void fullPageAndHtmxGetRenderTheFiveSessionTasks() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(get(BASE).session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("取り消せる削除")))
                .andExpect(content().string(containsString("data-status=\"ACTIVE\"")))
                .andExpect(content().string(containsString("name=\"revision\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("name=\"confirmDelete\"")))
                .andExpect(content().string(containsString("undoable-row-5")));

        mvc.perform(get(BASE).session(session).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(containsString("id=\"undoable-list\"")))
                .andExpect(content().string(containsString("undoable-row-1")));
    }

    @Test
    void deletionAndUndoReplaceOnlyTheRowAndRejectRepeatedStaleActions() throws Exception {
        var session = new MockHttpSession();
        String pendingRow = mvc.perform(post(BASE + "/1/delete").session(session).with(csrf().asHeader())
                        .header("HX-Request", "true").param("revision", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("data-status=\"PENDING_DELETE\"")))
                .andExpect(content().string(containsString("30秒以内に取り消せます")))
                .andReturn().getResponse().getContentAsString();
        long pendingRevision = revisionFrom(pendingRow);

        mvc.perform(post(BASE + "/1/delete").session(session).with(csrf().asHeader())
                        .header("HX-Request", "true").param("revision", "1"))
                .andExpect(status().isConflict());
        mvc.perform(post(BASE + "/1/undo").session(session).with(csrf().asHeader())
                        .header("HX-Request", "true").param("revision", "1"))
                .andExpect(status().isConflict());

        mvc.perform(get(BASE).session(session).header("HX-Request", "true"))
                .andExpect(content().string(containsString("data-status=\"PENDING_DELETE\"")))
                .andExpect(content().string(containsString("取り消す")));

        String activeRow = mvc.perform(post(BASE + "/1/undo").session(session).with(csrf().asHeader())
                        .header("HX-Request", "true")
                        .param("revision", Long.toString(pendingRevision)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-status=\"ACTIVE\"")))
                .andExpect(content().string(containsString("削除を取り消しました")))
                .andReturn().getResponse().getContentAsString();
        long activeRevision = revisionFrom(activeRow);
        assertThat(activeRevision).isNotEqualTo(pendingRevision);

        mvc.perform(post(BASE + "/1/undo").session(session).with(csrf().asHeader())
                        .header("HX-Request", "true")
                        .param("revision", Long.toString(pendingRevision)))
                .andExpect(status().isConflict());
    }

    @Test
    void deadlineIsInclusiveAndGetOnlyDerivesTheExpiredStatus() {
        var state = new UndoableDeleteState();
        Instant deletedAt = Instant.parse("2026-11-03T06:00:00Z");
        long activeRevision = state.pageSnapshot(deletedAt).tasks().getFirst().revision();
        var pending = state.requestDeletion(1, activeRevision, deletedAt);
        Instant deadline = deletedAt.plusSeconds(30);

        assertThat(state.pageSnapshot(deadline.minusNanos(1)).tasks().getFirst().status())
                .isEqualTo(UndoableDeleteState.Status.PENDING_DELETE);
        String pendingPageRevision = state.pageSnapshot(deletedAt).revision();
        assertThat(state.pageSnapshot(deadline).tasks().getFirst().status())
                .isEqualTo(UndoableDeleteState.Status.DELETED);
        assertThatThrownBy(() -> state.undoDeletion(1, pending.revision(), deadline))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> state.reset(pendingPageRevision, deadline))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(state.pageSnapshot(deadline).tasks().getFirst().status())
                .isEqualTo(UndoableDeleteState.Status.DELETED);
    }

    @Test
    void resetRestoresTasksWithNewRevisionsAndSessionsRemainIsolated() throws Exception {
        var firstSession = new MockHttpSession();
        var secondSession = new MockHttpSession();
        String originalRow = mvc.perform(get(BASE).session(firstSession))
                .andReturn().getResponse().getContentAsString();
        long oldRevision = revisionFrom(originalRow);

        mvc.perform(post(BASE + "/1/delete").session(firstSession).with(csrf().asHeader())
                        .header("HX-Request", "true").param("revision", Long.toString(oldRevision)))
                .andExpect(status().isOk());
        mvc.perform(get(BASE).session(secondSession))
                .andExpect(content().string(containsString("data-status=\"ACTIVE\"")));

        String latestPage = mvc.perform(get(BASE).session(firstSession))
                .andReturn().getResponse().getContentAsString();
        String latestListRevision = listRevisionFrom(latestPage);
        mvc.perform(post(BASE + "/reset").session(firstSession).with(csrf().asHeader())
                        .param("revision", latestListRevision)
                        .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-status=\"ACTIVE\"")));
        mvc.perform(post(BASE + "/1/delete").session(firstSession).with(csrf().asHeader())
                        .header("HX-Request", "true").param("revision", Long.toString(oldRevision)))
                .andExpect(status().isConflict());
    }

    @Test
    void normalFormsRequireConfirmationAndSupportDeleteUndoAndReset() throws Exception {
        var session = new MockHttpSession();
        long activeRevision = revisionFrom(mvc.perform(get(BASE).session(session))
                .andReturn().getResponse().getContentAsString());
        mvc.perform(post(BASE + "/1/delete").session(session).with(csrf())
                        .param("revision", Long.toString(activeRevision)))
                .andExpect(status().isBadRequest());
        mvc.perform(get(BASE).session(session))
                .andExpect(content().string(containsString("data-status=\"ACTIVE\"")));

        mvc.perform(post(BASE + "/1/delete").session(session).with(csrf())
                        .param("revision", Long.toString(activeRevision)).param("confirmDelete", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE));

        String pendingPage = mvc.perform(get(BASE).session(session))
                .andExpect(content().string(containsString("data-status=\"PENDING_DELETE\"")))
                .andReturn().getResponse().getContentAsString();
        long pendingRevision = revisionFrom(pendingPage);
        mvc.perform(post(BASE + "/1/undo").session(session).with(csrf())
                        .param("revision", Long.toString(pendingRevision)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE));
        mvc.perform(post(BASE + "/1/delete").session(session).with(csrf())
                        .param("revision", Long.toString(activeRevision)).param("confirmDelete", "true"))
                .andExpect(status().isConflict());

        String activePage = mvc.perform(get(BASE).session(session))
                .andReturn().getResponse().getContentAsString();
        String listRevision = listRevisionFrom(activePage);
        mvc.perform(post(BASE + "/reset").session(session).with(csrf())
                        .param("revision", listRevision))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE));
        mvc.perform(get(BASE).session(session))
                .andExpect(content().string(containsString("data-status=\"ACTIVE\"")));
    }

    @Test
    void staleResetDoesNotEraseChangesMadeFromAnotherTab() throws Exception {
        var session = new MockHttpSession();
        String staleListRevision = listRevisionFrom(mvc.perform(get(BASE).session(session))
                .andReturn().getResponse().getContentAsString());
        long rowRevision = revisionFrom(mvc.perform(get(BASE).session(session))
                .andReturn().getResponse().getContentAsString());
        mvc.perform(post(BASE + "/1/delete").session(session).with(csrf())
                        .param("revision", Long.toString(rowRevision)).param("confirmDelete", "true"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post(BASE + "/reset").session(session).with(csrf())
                        .param("revision", staleListRevision))
                .andExpect(status().isConflict());
        mvc.perform(get(BASE).session(session))
                .andExpect(content().string(containsString("data-status=\"PENDING_DELETE\"")));
    }

    @Test
    void allMutationsRejectMissingOrInvalidCsrf() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(post(BASE + "/1/delete").session(session).param("revision", "1")
                        .param("confirmDelete", "true"))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/1/delete").session(session).with(csrf().useInvalidToken())
                        .param("revision", "1").param("confirmDelete", "true"))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/1/undo").session(session))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/reset").session(session).param("revision", "1"))
                .andExpect(status().isForbidden());
        mvc.perform(get(BASE).session(session))
                .andExpect(content().string(containsString("data-status=\"ACTIVE\"")));
    }

    @Test
    void unknownIdsAndMalformedRevisionsFailWithoutChangingState() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(post(BASE + "/99/delete").session(session).with(csrf())
                        .param("revision", "1").param("confirmDelete", "true"))
                .andExpect(status().isNotFound());
        mvc.perform(post(BASE + "/1/delete").session(session).with(csrf())
                        .param("revision", "invalid").param("confirmDelete", "true"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(BASE).session(session))
                .andExpect(content().string(containsString("data-status=\"ACTIVE\"")));
    }

    private long revisionFrom(String html) {
        Matcher matcher = Pattern.compile("name=\"revision\" value=\"(\\d+)\"").matcher(html);
        assertThat(matcher.find()).isTrue();
        return Long.parseLong(matcher.group(1));
    }

    private String listRevisionFrom(String html) {
        Matcher matcher = Pattern.compile("name=\"revision\" value=\"([^\"]+)\"[^>]*>\\s*<button[^>]*>初期状態に戻す")
                .matcher(html);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
