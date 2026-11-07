package com.example.htmx;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** ページ全体・htmx断片・終端・通常フォームの代替を確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class ClickToLoadTests {
    private static final String BASE = "/examples/click-to-load";
    @Autowired MockMvc mvc;

    @Test
    void initialPageShowsFirstRowsAndAccessibleLoadControl() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("aria-label=\"サンプル\"")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("クリックで編集")))
                .andExpect(content().string(containsString("クリックして続きを読み込む")))
                .andExpect(content().string(containsString("佐藤 花子")))
                .andExpect(content().string(containsString("高橋 美咲")))
                .andExpect(content().string(not(containsString("田中 健"))))
                .andExpect(content().string(containsString("id=\"load-more\"")))
                .andExpect(content().string(containsString("hx-sync=\"this:drop\"")))
                .andExpect(content().string(containsString("hx-disabled-elt=\"find button\"")))
                .andExpect(content().string(containsString("hx-get=\"/examples/click-to-load\"")))
                .andExpect(content().string(containsString("method=\"get\"")))
                .andExpect(result -> assertEquals(1,
                        occurrences(result.getResponse().getContentAsString(), "佐藤 花子")));
    }

    @Test
    void htmxLoadsOnlyNextPageAndReplacesTheControlRow() throws Exception {
        mvc.perform(get(BASE).param("page", "2").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(containsString("田中 健")))
                .andExpect(content().string(containsString("渡辺 翔")))
                .andExpect(content().string(not(containsString("佐藤 花子"))))
                .andExpect(content().string(containsString("id=\"load-more\"")));
    }

    @Test
    void finalPageHasNoFurtherLoadControl() throws Exception {
        mvc.perform(get(BASE).param("page", "3").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("山本 凛")))
                .andExpect(content().string(containsString("中村 陸")))
                .andExpect(content().string(not(containsString("id=\"load-more\""))));
    }

    @Test
    void normalGetFallbackKeepsAllRowsThroughRequestedPage() throws Exception {
        mvc.perform(get(BASE).param("page", "2").session(new MockHttpSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("佐藤 花子")))
                .andExpect(content().string(containsString("渡辺 翔")))
                .andExpect(content().string(not(containsString("山本 凛"))))
                .andExpect(content().string(containsString("action=\"/examples/click-to-load\"")))
                .andExpect(content().string(containsString("name=\"page\" value=\"3\"")));
        mvc.perform(get(BASE).param("page", "3").session(new MockHttpSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("中村 陸")))
                .andExpect(content().string(not(containsString("id=\"load-more\""))));
    }

    @Test
    void pageNumberMustBeWithinTheAvailableRange() throws Exception {
        mvc.perform(get(BASE).param("page", "0")).andExpect(status().isNotFound());
        mvc.perform(get(BASE).param("page", "4")).andExpect(status().isNotFound());
        mvc.perform(get(BASE).param("page", "invalid")).andExpect(status().isBadRequest());
    }

    private static int occurrences(String text, String value) {
        return text.split(java.util.regex.Pattern.quote(value), -1).length - 1;
    }
}
