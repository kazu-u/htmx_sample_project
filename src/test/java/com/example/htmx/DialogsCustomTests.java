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

/** 独自ダイアログのページ、取得断片、JavaScript 無効時の代替ページを確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class DialogsCustomTests {
    private static final String BASE = "/examples/dialogs-custom";
    private static final String DETAIL = BASE + "/detail";

    @Autowired
    MockMvc mvc;

    @Test
    void pageIncludesNativeDialogAndLocalScriptsAndStyles() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("<dialog id=\"details-dialog\"")))
                .andExpect(content().string(containsString("form method=\"dialog\"")))
                .andExpect(content().string(containsString("/js/dialogs-custom.js")))
                .andExpect(content().string(containsString("/css/dialogs-custom.css")))
                .andExpect(content().string(containsString("hx-get=\"/examples/dialogs-custom/detail\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void htmxDetailRequestReturnsOnlyTheDialogContentFragment() throws Exception {
        mvc.perform(get(DETAIL).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("id=\"detail-heading\"")))
                .andExpect(content().string(containsString("架空の案内")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("<script"))));
    }

    @Test
    void normalDetailRequestReturnsACompleteFallbackPage() throws Exception {
        mvc.perform(get(DETAIL))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("ダイアログの代替ページ")))
                .andExpect(content().string(containsString("id=\"detail-heading\"")))
                .andExpect(content().string(containsString("href=\"/examples/dialogs-custom\"")));
    }
}
