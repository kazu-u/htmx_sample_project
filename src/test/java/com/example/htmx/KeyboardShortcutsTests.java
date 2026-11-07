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

/** キーボード操作ページと通常・htmx GET 応答を確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class KeyboardShortcutsTests {
    private static final String BASE = "/examples/keyboard-shortcuts";
    private static final String ACTION = BASE + "/action";

    @Autowired
    MockMvc mvc;

    @Test
    void pageRendersButtonFallbackAndExternalKeyHandler() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString(
                        "method=\"get\" action=\"/examples/keyboard-shortcuts/action\"")))
                .andExpect(content().string(containsString(
                        "hx-get=\"/examples/keyboard-shortcuts/action\"")))
                .andExpect(content().string(containsString("案内を取得する（Alt+Shift+D）")))
                .andExpect(content().string(containsString("/js/keyboard-shortcuts.js")))
                .andExpect(content().string(containsString("allowEval\":false")))
                .andExpect(content().string(not(containsString("hx-trigger="))))
                .andExpect(content().string(not(containsString("hx-on:"))))
                .andExpect(content().string(not(containsString("onclick="))));
    }

    @Test
    void htmxActionReturnsOnlyResultPanel() throws Exception {
        mvc.perform(get(ACTION).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(containsString("id=\"shortcut-result\"")))
                .andExpect(content().string(containsString(
                        "サーバー状態は変更していません。")));
    }

    @Test
    void ordinaryActionReturnsCompletePageWithResult() throws Exception {
        mvc.perform(get(ACTION))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString(
                        "サーバー状態は変更していません。")));
    }
}
