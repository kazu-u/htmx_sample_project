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

/** 独自確認 UI、要求結果の断片、通常アクセスの代替ページを確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class ConfirmTests {
    private static final String BASE = "/examples/confirm";
    private static final String RESULT = BASE + "/result";

    @Autowired
    MockMvc mvc;

    @Test
    void pageProvidesCustomDialogAndNoJavaScriptFallback() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("<dialog id=\"confirmation-dialog\"")))
                .andExpect(content().string(containsString("hx-confirm=\"確認後の結果を取得します。続けますか？\"")))
                .andExpect(content().string(containsString("data-custom-confirm")))
                .andExpect(content().string(containsString("/js/confirm.js")))
                .andExpect(content().string(containsString("/css/confirm.css")))
                .andExpect(content().string(containsString("href=\"/examples/confirm/result\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void htmxResultRequestReturnsOnlyTheResultFragment() throws Exception {
        mvc.perform(get(RESULT).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("id=\"confirmed-result\"")))
                .andExpect(content().string(containsString("サーバー状態は変更していません")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("<script"))));
    }

    @Test
    void normalResultRequestReturnsACompleteFallbackPage() throws Exception {
        mvc.perform(get(RESULT))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("id=\"confirmed-result\"")))
                .andExpect(content().string(containsString("href=\"/examples/confirm\"")));
    }
}
