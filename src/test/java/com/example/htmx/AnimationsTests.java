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

/** 固定候補の全体表示・断片応答・不正 ID の拒否を確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class AnimationsTests {
    private static final String BASE = "/examples/animations";

    @Autowired MockMvc mvc;

    @Test
    void pageIncludesInitialCardAndMotionPreferenceSupport() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("id=\"animation-card\"")))
                .andExpect(content().string(containsString("珊瑚色のカード")))
                .andExpect(content().string(containsString("/css/animations.css")))
                .andExpect(content().string(containsString("aria-label=\"カードの色を選ぶ\"")))
                .andExpect(content().string(containsString("hx-swap=\"outerHTML\"")))
                .andExpect(content().string(containsString("name=\"id\"")))
                .andExpect(content().string(containsString("id=\"animation-coral-button\"")))
                .andExpect(content().string(containsString("aria-current=\"page\"")));
    }

    @Test
    void htmxReceivesOnlyAStableIdCardFragment() throws Exception {
        mvc.perform(get(BASE + "/card").param("id", "ocean").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("id=\"animation-card\"")))
                .andExpect(content().string(containsString("animation-card--ocean")))
                .andExpect(content().string(containsString("深い青色のカードです。")))
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    void ordinaryFormReceivesTheWholePage() throws Exception {
        mvc.perform(get(BASE + "/card").param("id", "forest"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("森林色のカード")));
    }

    @Test
    void unknownAppearanceIsNotRenderedAsSuccessfulContent() throws Exception {
        mvc.perform(get(BASE + "/card").param("id", "unknown").header("HX-Request", "true"))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingAppearanceIdIsRejected() throws Exception {
        mvc.perform(get(BASE + "/card").header("HX-Request", "true"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void stylesheetContainsReducedMotionAndSwapStyles() throws Exception {
        mvc.perform(get("/css/animations.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(".animation-card.htmx-added")))
                .andExpect(content().string(containsString("transition: opacity 240ms ease;")))
                .andExpect(content().string(containsString("prefers-reduced-motion: reduce")))
                .andExpect(content().string(containsString("transition-duration: 0.01ms")));
    }
}
