package com.example.htmx;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 初期HTMLと断片の分離、通常リンク代替、安全設定、固定データの参照を確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class LazyLoadingTests {
    private static final String BASE = "/examples/lazy-loading";
    @Autowired MockMvc mvc;

    @Test
    void initialPageContainsOneLoadTriggerButNoSummary() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(matchesPattern("(?s).*<section[^>]*id=\"lazy-panel\"[^>]*hx-trigger=\"load\"[^>]*>.*")))
                .andExpect(content().string(containsString("hx-sync=\"#lazy-panel:drop\"")))
                .andExpect(content().string(containsString("id=\"lazy-retry\"")))
                .andExpect(content().string(containsString("集計はまだ取得していません。")))
                .andExpect(content().string(not(containsString("<table data-lazy-summary"))))
                .andExpect(content().string(containsString("\"allowEval\":false")))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")));
    }

    @Test
    void fragmentContainsOnlyFixedSummaryWithNoFetchOrScript() throws Exception {
        mvc.perform(get(BASE + "/summary").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("<table data-lazy-summary")))
                .andExpect(content().string(containsString("12件")))
                .andExpect(content().string(containsString("max=\"20\"")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("<script"))))
                .andExpect(content().string(not(containsString("hx-get"))))
                .andExpect(content().string(not(containsString("hx-trigger"))));
    }

    @Test
    void normalSummaryUrlReturnsFullLoadedPageWithoutAnotherAutomaticFetch() throws Exception {
        mvc.perform(get(BASE + "/summary"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("<table data-lazy-summary")))
                .andExpect(content().string(not(matchesPattern("(?s).*<section[^>]*id=\"lazy-panel\"[^>]*hx-trigger=\"load\"[^>]*>.*"))))
                .andExpect(header().string("Vary", containsString("HX-Request")));
    }

    @Test
    void repeatedGetsAndOtherSessionsHaveTheSameFixedSummaryAndNoBusinessState() throws Exception {
        var session = new MockHttpSession();
        String first = mvc.perform(get(BASE + "/summary").session(session).header("HX-Request", "true"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (var target : java.util.List.of(session, session, new MockHttpSession())) {
            String html = mvc.perform(get(BASE + "/summary").session(target).header("HX-Request", "true"))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(html).isEqualTo(first);
            assertThat(java.util.Collections.list(target.getAttributeNames()))
                    .noneMatch(name -> name.startsWith("scopedTarget."));
        }
    }
}
