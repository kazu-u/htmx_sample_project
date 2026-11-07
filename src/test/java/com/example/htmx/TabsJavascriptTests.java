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

/** タブ画面の初期状態、部分応答、通常リンク、未知 ID を確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class TabsJavascriptTests {
    private static final String BASE = "/examples/tabs-javascript";
    private static final String MECHANISM_TAB = BASE + "/tab/mechanism";

    @Autowired
    MockMvc mvc;

    @Test
    void pageRendersAccessibleTabsAndInitialPanel() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("id=\"tabs-javascript-list\"")))
                .andExpect(content().string(containsString(
                        "href=\"/examples/tabs-javascript/tab/overview\"")))
                .andExpect(content().string(containsString(
                        "hx-get=\"/examples/tabs-javascript/tab/overview\" aria-selected=\"true\"")))
                .andExpect(content().string(containsString(
                        "hx-get=\"/examples/tabs-javascript/tab/mechanism\" aria-selected=\"false\"")))
                .andExpect(content().string(containsString(
                        "hx-get=\"/examples/tabs-javascript/tab/limits\" aria-selected=\"false\"")))
                .andExpect(content().string(containsString("<h3>概要</h3>")))
                .andExpect(content().string(containsString(
                        "aria-labelledby=\"tabs-javascript-tab-overview\"")))
                .andExpect(content().string(not(containsString("tabindex=\"-1\""))))
                .andExpect(content().string(containsString("/js/tabs-javascript.js")))
                .andExpect(content().string(containsString("allowEval\":false")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString(
                        "ベース URL を再読み込みすると概要に戻ります。")));
    }

    @Test
    void htmxTabRequestReturnsOnlySelectedPanel() throws Exception {
        mvc.perform(get(MECHANISM_TAB).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(containsString("id=\"tabs-javascript-panel\"")))
                .andExpect(content().string(containsString("aria-labelledby=\"tabs-javascript-tab-mechanism\"")))
                .andExpect(content().string(containsString("htmx が GET で取得した HTML を差し替え")));
    }

    @Test
    void directTabRequestReturnsCompletePageWithRequestedTabSelected() throws Exception {
        mvc.perform(get(MECHANISM_TAB))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString(
                        "hx-get=\"/examples/tabs-javascript/tab/mechanism\" aria-selected=\"true\"")))
                .andExpect(content().string(containsString("aria-labelledby=\"tabs-javascript-tab-mechanism\"")));
    }

    @Test
    void tabRequestsDoNotAddHistoryOrUseInlineEventHandlers() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("hx-push-url"))))
                .andExpect(content().string(not(containsString("hx-on:"))))
                .andExpect(content().string(not(containsString("onclick="))));
    }

    @Test
    void unknownTabIdReturnsNotFound() throws Exception {
        mvc.perform(get(BASE + "/tab/unknown").header("HX-Request", "true"))
                .andExpect(status().isNotFound());
    }
}
