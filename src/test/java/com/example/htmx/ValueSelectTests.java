package com.example.htmx;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 親に応じた固定候補、選択検証、ページ全体と断片の応答を確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class ValueSelectTests {
    private static final String BASE = "/examples/value-select";

    @Autowired MockMvc mvc;

    @Test
    void initialPageHasUnselectedParentAndDisabledChild() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("hx-get=\"" + BASE + "/cities\"")))
                .andExpect(content().string(containsString("hx-target=\"#city-field\"")))
                .andExpect(content().string(containsString(
                        "<option value=\"\" selected=\"selected\">選択してください</option>")))
                .andExpect(content().string(containsString("id=\"city-select\" name=\"cityId\"")))
                .andExpect(content().string(containsString("disabled=\"disabled\"")))
                .andExpect(content().string(containsString("まだ結果を確認していません。")))
                .andExpect(content().string(not(containsString("type=\"hidden\" name=\"_csrf\""))));
    }

    @Test
    void selectingAParentReturnsOnlyItsChildOptionsAndClearsPriorResult() throws Exception {
        mvc.perform(get(BASE + "/cities").param("prefectureId", "aoba").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("id=\"city-field\"")))
                .andExpect(content().string(containsString("青葉市")))
                .andExpect(content().string(containsString("若葉市")))
                .andExpect(content().string(containsString("value=\"\" selected=\"selected\"")))
                .andExpect(content().string(not(containsString("星見市"))))
                .andExpect(content().string(containsString("id=\"selection-result\"")))
                .andExpect(content().string(containsString("hx-swap-oob=\"outerHTML\"")))
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    void clearingParentResetsAndDisablesChildWithoutChangingServerState() throws Exception {
        mvc.perform(get(BASE + "/cities").param("prefectureId", "aoba").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("青葉市")));
        mvc.perform(get(BASE + "/cities").param("prefectureId", "").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("disabled=\"disabled\"")))
                .andExpect(content().string(not(containsString("青葉市"))));
    }

    @Test
    void normalFormCanLoadOptionsAndConfirmSelection() throws Exception {
        mvc.perform(get(BASE).param("action", "loadCities").param("prefectureId", "hoshimi"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("星見県")))
                .andExpect(content().string(containsString("星見市")))
                .andExpect(content().string(containsString("id=\"city-select\"")));
        mvc.perform(get(BASE).param("action", "confirm")
                        .param("prefectureId", "hoshimi").param("cityId", "tenkuu-city"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("天空市")))
                .andExpect(content().string(containsString("星見県")));
    }

    @Test
    void validHtmxConfirmationReturnsOnlyTheResultFragment() throws Exception {
        mvc.perform(get(BASE).param("action", "confirm")
                        .param("prefectureId", "asanagi").param("cityId", "umikaze-town")
                        .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"selection-result\"")))
                .andExpect(content().string(containsString("朝凪県")))
                .andExpect(content().string(containsString("海風町")))
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    void missingAndMismatchedValuesReturn422AndResetInvalidChild() throws Exception {
        mvc.perform(get(BASE).param("action", "confirm"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("都道府県を選択してください。")))
                .andExpect(content().string(not(containsString("選択内容を確認してください。"))));
        mvc.perform(get(BASE).param("action", "confirm").header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("id=\"selection-error\"")))
                .andExpect(content().string(containsString("都道府県を選択してください。")))
                .andExpect(content().string(containsString("id=\"prefecture-field\"")))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")))
                .andExpect(content().string(containsString("hx-swap-oob=\"outerHTML\"")));
        mvc.perform(get(BASE).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(containsString("都道府県を選択してください。")));
        mvc.perform(get(BASE).param("action", "loadCities").header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("id=\"prefecture-field\"")))
                .andExpect(content().string(containsString("都道府県を選択してください。")));
        mvc.perform(get(BASE).param("action", "confirm")
                        .param("prefectureId", "aoba").param("cityId", "hoshimi-city")
                        .header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("id=\"selection-error\"")))
                .andExpect(content().string(containsString("選択した市区町村は、この都道府県に属していません。")))
                .andExpect(content().string(containsString("id=\"city-field\"")))
                .andExpect(content().string(containsString("hx-swap-oob=\"outerHTML\"")))
                .andExpect(content().string(not(containsString("hoshimi-city"))));
        mvc.perform(get(BASE).param("action", "confirm")
                        .param("prefectureId", "aoba").param("cityId", "hoshimi-city"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("value=\"aoba\"")))
                .andExpect(content().string(containsString("selected=\"selected\"")))
                .andExpect(content().string(containsString("選択した市区町村は、この都道府県に属していません。")));
        mvc.perform(get(BASE).param("action", "confirm")
                        .param("prefectureId", "aoba").param("cityId", ""))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("市区町村を選択してください。")));
    }

    @Test
    void unknownIdsAreRejectedWithoutEchoingInputAndDoNotDependOnSession() throws Exception {
        String hostileId = "<script>alert(1)</script>";
        mvc.perform(get(BASE + "/cities").param("prefectureId", hostileId).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("都道府県を選び直してください。")))
                .andExpect(content().string(containsString("id=\"prefecture-field\"")))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")))
                .andExpect(content().string(not(containsString("<script>"))));
        mvc.perform(get(BASE).param("action", "confirm")
                        .param("prefectureId", "aoba").param("cityId", hostileId))
                .andExpect(status().is(422))
                .andExpect(content().string(not(containsString("<script>"))));
        mvc.perform(get(BASE).session(new MockHttpSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("disabled=\"disabled\"")));
    }
}
