package com.example.htmx;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 独立したカード断片、通常ページ代替、安全設定を確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardTests {
    private static final String BASE = "/examples/dashboard";

    @Autowired MockMvc mvc;

    @Test
    void pageRendersTwoIndependentPollingCardsAndControls() throws Exception {
        var result = mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("hx-trigger=\"every 5s, dashboard-refresh\"")))
                .andExpect(content().string(containsString("hx-sync=\"closest [data-dashboard-card]:drop\"")))
                .andExpect(content().string(containsString("id=\"dashboard-auto-toggle\"")))
                .andExpect(content().string(containsString("表示中の累計5分")))
                .andExpect(content().string(containsString("/js/dashboard-polling.js")))
                .andExpect(content().string(containsString("/css/dashboard.css")))
                .andExpect(content().string(containsString("id=\"dashboard-auto-status\" role=\"status\" aria-live=\"polite\"")))
                .andExpect(content().string(containsString("\"allowEval\":false")))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andReturn();
        String html = result.getResponse().getContentAsString();
        assertThat(Pattern.compile("id=\"(?:orders|inventory)-panel\"").matcher(html).results().count()).isEqualTo(2);
        assertThat(Pattern.compile("data-dashboard-poll").matcher(html).results().count()).isEqualTo(2);
        assertThat(html).contains("本日の受付", "利用可能な在庫", "最終成功取得");
    }

    @Test
    void eachHtmxRequestReturnsOnlyItsCardContent() throws Exception {
        mvc.perform(get(BASE + "/summary/orders").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("処理待ち")))
                .andExpect(content().string(containsString("最終成功取得")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("hx-trigger"))));
        mvc.perform(get(BASE + "/summary/inventory").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("在庫確認が必要な品目")))
                .andExpect(content().string(containsString("最終成功取得")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("hx-trigger"))));
    }

    @Test
    void normalRefreshLinksReturnTheWholeDashboard() throws Exception {
        mvc.perform(get(BASE + "/summary/orders"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("本日の受付")))
                .andExpect(content().string(containsString("利用可能な在庫")));
        mvc.perform(get(BASE + "/summary/inventory"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("在庫確認が必要な品目")));
    }

    @Test
    void dashboardPollingScriptIsServedAsAnExternalResource() throws Exception {
        mvc.perform(get("/js/dashboard-polling.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("visibilitychange")))
                .andExpect(content().string(containsString("maximumVisibleTime")));
    }
}
