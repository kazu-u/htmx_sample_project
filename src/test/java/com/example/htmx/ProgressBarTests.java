package com.example.htmx;

import com.example.htmx.examples.progressbar.ProgressJobState;
import java.time.Instant;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
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

/** 開始・参照・終了の分離、CSRF、時間ベース進捗、セッション境界を確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class ProgressBarTests {
    private static final String BASE = "/examples/progress-bar";

    @Autowired MockMvc mvc;

    @Test
    void initialPageShowsIdleFormWithoutAutomaticPolling() throws Exception {
        var result = mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("data-job-status=\"IDLE\"")))
                .andExpect(content().string(containsString("成功する処理を開始")))
                .andExpect(content().string(containsString("\"allowEval\":false")))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andReturn();
        assertThat(progressState(result.getResponse().getContentAsString()))
                .doesNotContain("hx-trigger=\"every 1s\"");
        String html = result.getResponse().getContentAsString();
        assertThat(Pattern.compile("id=\"progress-panel\"").matcher(html).results().count()).isEqualTo(1);
        assertThat(Pattern.compile("id=\"progress-state\"").matcher(html).results().count()).isEqualTo(1);
    }

    @Test
    void startsAndPollsAJobButKeepsProgressGetReadOnly() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(post(BASE + "/start").session(session).with(csrf()).header("HX-Request", "true")
                        .param("outcome", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("data-job-status=\"RUNNING\"")))
                .andExpect(content().string(containsString("hx-get=\"/examples/progress-bar/progress\"")))
                .andExpect(content().string(containsString("hx-trigger=\"every 1s\"")))
                .andExpect(content().string(not(containsString("<html"))));

        mvc.perform(get(BASE + "/progress").session(session).header("HX-Request", "true"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("data-job-status=\"RUNNING\"")))
                .andReturn();
        String second = mvc.perform(get(BASE + "/progress").session(session).header("HX-Request", "true"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(second).contains("data-job-status=\"RUNNING\"");
        assertThat(second).contains("hx-trigger=\"every 1s\"");
        assertThat(second).doesNotContain("cancel-job-button");

        String manualRefresh = mvc.perform(get(BASE + "/progress").session(session).header("HX-Request", "true")
                        .param("includeControls", "true"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(manualRefresh).contains("data-job-status=\"RUNNING\"");
        assertThat(manualRefresh).contains("id=\"job-controls\" hx-swap-oob=\"outerHTML\"");
        assertThat(manualRefresh).contains("cancel-job-button");
    }

    @Test
    void cancellationStopsPollingAndConcurrentStartIsRejected() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(post(BASE + "/start").session(session).with(csrf()).header("HX-Request", "true")
                        .param("outcome", "FAILURE"))
                .andExpect(status().isOk());
        mvc.perform(post(BASE + "/cancel").session(session).header("HX-Request", "true"))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/start").session(session).with(csrf()).header("HX-Request", "true")
                        .param("outcome", "SUCCESS"))
                .andExpect(status().isConflict());
        var cancelled = mvc.perform(post(BASE + "/cancel").session(session).with(csrf()).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-job-status=\"CANCELLED\"")))
                .andExpect(content().string(containsString("処理をキャンセルしました。")))
                .andReturn();
        assertThat(progressState(cancelled.getResponse().getContentAsString()))
                .doesNotContain("hx-trigger=\"every 1s\"");
        mvc.perform(post(BASE + "/cancel").session(session).with(csrf()).header("HX-Request", "true"))
                .andExpect(status().isConflict());
    }

    @Test
    void startRequiresCsrfAndNormalFormRedirects() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(post(BASE + "/start").session(session).param("outcome", "SUCCESS"))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/cancel").session(session))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/start").session(session).with(csrf()).param("outcome", "SUCCESS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE));
        mvc.perform(get(BASE).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-job-status=\"RUNNING\"")));
        mvc.perform(post(BASE + "/cancel").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE));
        mvc.perform(get(BASE).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-job-status=\"CANCELLED\"")));
    }

    @Test
    void jobsAreIsolatedBySession() throws Exception {
        var started = new MockHttpSession();
        var untouched = new MockHttpSession();
        mvc.perform(post(BASE + "/start").session(started).with(csrf()).header("HX-Request", "true")
                        .param("outcome", "SUCCESS"))
                .andExpect(status().isOk());
        var result = mvc.perform(get(BASE).session(untouched))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-job-status=\"IDLE\"")))
                .andReturn();
        assertThat(progressState(result.getResponse().getContentAsString()))
                .doesNotContain("hx-trigger=\"every 1s\"");
    }

    @Test
    void progressIsDerivedFromTimeAndSuccessOrFailureStopsAtOneHundred() {
        var state = new ProgressJobState();
        var start = Instant.parse("2026-10-31T00:00:00Z");
        assertThat(state.start(ProgressJobState.Outcome.SUCCESS, start).progress()).isZero();
        var halfway = state.snapshot(start.plusSeconds(5));
        assertThat(halfway.progress()).isEqualTo(50);
        assertThat(state.snapshot(start.plusSeconds(5))).isEqualTo(halfway);
        assertThat(state.snapshot(start.plusSeconds(10)).status()).isEqualTo(ProgressJobState.Status.COMPLETED);
        assertThat(state.snapshot(start.plusSeconds(10)).progress()).isEqualTo(100);

        state.start(ProgressJobState.Outcome.FAILURE, start.plusSeconds(20));
        var failed = state.snapshot(start.plusSeconds(30));
        assertThat(failed.status()).isEqualTo(ProgressJobState.Status.FAILED);
        assertThat(failed.progress()).isEqualTo(100);

        state.start(ProgressJobState.Outcome.SUCCESS, start.plusSeconds(40));
        state.cancel(start.plusSeconds(44));
        assertThat(state.snapshot(start.plusSeconds(48)).progress()).isEqualTo(40);
    }

    private String progressState(String html) {
        var matcher = Pattern.compile("<div id=\"progress-state\"[^>]*>").matcher(html);
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }
}
