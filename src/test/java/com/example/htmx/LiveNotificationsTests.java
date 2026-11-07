package com.example.htmx;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 初期ページ、安全なローカル拡張、SSE形式、再開位置の検証を行う。 */
@SpringBootTest
@AutoConfigureMockMvc
class LiveNotificationsTests {
    private static final String BASE = "/examples/live-notifications";

    @Autowired
    MockMvc mvc;

    @Test
    void pageRendersAnAccessibleSseStreamAndNoScriptFallback() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("hx-ext=\"sse\"")))
                .andExpect(content().string(containsString("sse-connect=\"/examples/live-notifications/stream\"")))
                .andExpect(content().string(containsString("sse-swap=\"notification\"")))
                .andExpect(content().string(containsString("hx-swap=\"beforeend\"")))
                .andExpect(content().string(containsString("sse-close=\"finished\"")))
                .andExpect(content().string(containsString("aria-relevant=\"additions\"")))
                .andExpect(content().string(containsString("JavaScript が無効")))
                .andExpect(content().string(containsString("/vendor/htmx-ext-sse/2.2.4/sse.min.js")))
                .andExpect(content().string(containsString("/js/live-notifications.js")))
                .andExpect(content().string(not(containsString("cdn.jsdelivr.net"))))
                .andExpect(content().string(containsString("\"allowEval\":false")))
                .andExpect(header().string("Content-Security-Policy", containsString("connect-src 'self'")));
    }

    @Test
    void streamResumesAfterLastReceivedEventAndClosesExplicitly() throws Exception {
        MvcResult pending = mvc.perform(get(BASE + "/stream")
                        .header("Last-Event-ID", "4")
                        .param("lastEventId", "2"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(pending))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/event-stream"))
                .andExpect(content().contentTypeCompatibleWith("text/event-stream;charset=UTF-8"))
                .andExpect(content().string(containsString("id:5")))
                .andExpect(content().string(containsString("event:notification")))
                .andExpect(content().string(containsString("ORD-1043")))
                .andExpect(content().string(containsString("確認が必要です")))
                .andExpect(content().string(containsString("event:finished")))
                .andExpect(header().string("Cache-Control", containsString("no-cache")));
    }

    @Test
    void recreatedEventSourceCanResumeFromValidatedQueryCursor() throws Exception {
        MvcResult pending = mvc.perform(get(BASE + "/stream").param("lastEventId", "2"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(pending))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id:3")))
                .andExpect(content().string(containsString("id:5")))
                .andExpect(content().string(not(containsString("id:1"))))
                .andExpect(content().string(containsString("ORD-1043")))
                .andExpect(content().string(containsString("event:finished")));
    }

    @Test
    void invalidResumeIdsAreRejected() throws Exception {
        for (String invalidId : new String[] {"-1", "6", "999999999999999999999", "+1", "1.0"}) {
            mvc.perform(get(BASE + "/stream").header("Last-Event-ID", invalidId))
                    .andExpect(status().isBadRequest());
            mvc.perform(get(BASE + "/stream").param("lastEventId", invalidId))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void extensionAndLicenseAreServedLocally() throws Exception {
        mvc.perform(get("/vendor/htmx-ext-sse/2.2.4/sse.min.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("defineExtension")));
        mvc.perform(get("/vendor/htmx-ext-sse/2.2.4/LICENSE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("BSD Zero Clause License")));
    }
}
