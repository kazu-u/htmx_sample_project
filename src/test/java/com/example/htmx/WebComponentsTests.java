package com.example.htmx;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Shadow DOM の初期化案内、断片応答、通常ページ代替を確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class WebComponentsTests {
    private static final String BASE = "/examples/web-components";

    @Autowired
    MockMvc mvc;

    @Test
    void pageRendersIndependentComponentsAndSafeFallbackLinks() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("<htmx-summary-card topic=\"inventory\">")))
                .andExpect(content().string(containsString("<htmx-summary-card topic=\"orders\">")))
                .andExpect(content().string(containsString("id=\"summary-card-template\"")))
                .andExpect(content().string(containsString("htmx.process(shadowRoot)")))
                .andExpect(content().string(containsString("hx-target=\"next .summary-result\"")))
                .andExpect(content().string(containsString("/js/web-components.js")))
                .andExpect(content().string(containsString("/css/web-components.css")))
                .andExpect(content().string(containsString("href=\"/examples/web-components/summary/inventory\"")))
                .andExpect(content().string(containsString("href=\"/examples/web-components/summary/orders\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("<script>"))));
    }

    @Test
    void htmxRequestReturnsOnlyTheSelectedSummaryFragment() throws Exception {
        mvc.perform(get(BASE + "/summary/inventory").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("書籍 24 冊")))
                .andExpect(content().string(containsString("学習用の固定データです。")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("<script"))));
    }

    @Test
    void normalRequestReturnsACompleteSummaryPage() throws Exception {
        mvc.perform(get(BASE + "/summary/orders"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("本日の注文 12 件")))
                .andExpect(content().string(containsString("Web Components のサンプルに戻る")));
    }

    @Test
    void unknownSummaryTopicReturnsNotFound() throws Exception {
        mvc.perform(get(BASE + "/summary/unknown").header("HX-Request", "true"))
                .andExpect(status().isNotFound());
    }

    @Test
    void componentScriptIsServedAsAnExternalResource() throws Exception {
        mvc.perform(get("/js/web-components.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("htmx.process(shadowRoot)")))
                .andExpect(content().string(containsString("event.stopPropagation()")));
    }
}
