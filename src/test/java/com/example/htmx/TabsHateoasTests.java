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

/** タブの選択状態を含む断片、通常ページ、履歴復元、未知 ID の応答を確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class TabsHateoasTests {
    private static final String BASE = "/examples/tabs-hateoas";
    private static final String MECHANISM_TAB = BASE + "/tab/mechanism";

    @Autowired
    MockMvc mvc;

    @Test
    void pageRendersInitialTabWithAccessibleLinksAndLocalAssets() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("id=\"tab-interface\"")))
                .andExpect(content().string(containsString("href=\"/examples/tabs-hateoas/tab/overview\"")))
                .andExpect(content().string(containsString("aria-selected=\"true\"")))
                .andExpect(content().string(containsString("aria-selected=\"false\"")))
                .andExpect(content().string(containsString("tabindex=\"0\"")))
                .andExpect(content().string(containsString("tabindex=\"-1\"")))
                .andExpect(content().string(containsString("/js/tabs-hateoas.js")))
                .andExpect(content().string(containsString("/css/tabs-hateoas.css")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void htmxTabRequestReturnsSelectedTabAndAllNavigationLinks() throws Exception {
        mvc.perform(get(MECHANISM_TAB).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(header().string("Vary", containsString("HX-History-Restore-Request")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(containsString("aria-selected=\"true\"")))
                .andExpect(content().string(containsString("id=\"tab-link-mechanism\"")))
                .andExpect(content().string(containsString("tabindex=\"0\"")))
                .andExpect(content().string(containsString("各タブは同一オリジンの GET リンクです。")))
                .andExpect(content().string(containsString("href=\"/examples/tabs-hateoas/tab/overview\"")))
                .andExpect(content().string(containsString("href=\"/examples/tabs-hateoas/tab/limits\"")));
    }

    @Test
    void directTabRequestReturnsCompletePageWithRequestedTabSelected() throws Exception {
        mvc.perform(get(MECHANISM_TAB))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString(
                        "hx-get=\"/examples/tabs-hateoas/tab/mechanism\" aria-selected=\"true\"")))
                .andExpect(content().string(containsString("各タブは同一オリジンの GET リンクです。")));
    }

    @Test
    void historyRestoreRequestReturnsCompletePageForHtmxToRestore() throws Exception {
        mvc.perform(get(MECHANISM_TAB)
                        .header("HX-Request", "true")
                        .header("HX-History-Restore-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-History-Restore-Request")))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString(
                        "hx-get=\"/examples/tabs-hateoas/tab/mechanism\" aria-selected=\"true\"")));
    }

    @Test
    void unknownTabIdReturnsNotFound() throws Exception {
        mvc.perform(get(BASE + "/tab/unknown").header("HX-Request", "true"))
                .andExpect(status().isNotFound());
    }
}
