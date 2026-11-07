package com.example.htmx;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 候補の上限・入力検証・ID照合・通常フォームを確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class AccessibleComboboxTests {
    private static final String BASE = "/examples/accessible-combobox";

    @Autowired MockMvc mvc;

    @Test
    void pageProvidesComboboxEnhancementAndNoScriptFallback() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("method=\"get\"")))
                .andExpect(content().string(containsString("id=\"candidate-query\"")))
                .andExpect(content().string(containsString("hx-trigger=\"combobox-search\"")))
                .andExpect(content().string(containsString("id=\"candidate-fallback\"")))
                .andExpect(content().string(containsString("/js/accessible-combobox.js")))
                .andExpect(content().string(containsString("allowEval\":false")))
                .andExpect(content().string(containsString("aria-current=\"page\"")));
    }

    @Test
    void candidateEndpointReturnsOnlyMatchingOptionsAndCapsAtTen() throws Exception {
        mvc.perform(get(BASE + "/candidates").param("query", "佐藤"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("candidates", hasSize(1)))
                .andExpect(content().string(containsString("佐藤 花子")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("<script"))));

        mvc.perform(get(BASE + "/candidates").param("query", "チーム"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("candidates", hasSize(10)))
                .andExpect(content().string(containsString("候補は10件です。")));
    }

    @Test
    void blankQueryAndNoMatchesReturnAnEmptyCandidateList() throws Exception {
        mvc.perform(get(BASE + "/candidates"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("candidates", hasSize(0)))
                .andExpect(content().string(containsString("候補はありません")));
        mvc.perform(get(BASE + "/candidates").param("query", "該当なし"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("candidates", hasSize(0)));
    }

    @Test
    void queryLengthBoundaryIsValidatedOnPartialAndFullRequests() throws Exception {
        mvc.perform(get(BASE + "/candidates").param("query", "あ".repeat(60)))
                .andExpect(status().isOk());
        mvc.perform(get(BASE + "/candidates").param("query", "あ".repeat(61)))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("data-candidate-error")))
                .andExpect(content().string(containsString("検索語は60文字以内で入力してください。")));
        mvc.perform(get(BASE).param("query", "あ".repeat(61)))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")));
    }

    @Test
    void validSelectedIdIsResolvedFromFixedData() throws Exception {
        mvc.perform(get(BASE).param("action", "choose").param("query", "佐藤")
                        .param("contactId", "person-01"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("佐藤 花子")))
                .andExpect(content().string(containsString("を選択しました。")));
    }

    @Test
    void noScriptFallbackCanSelectAndConfirmCandidate() throws Exception {
        mvc.perform(get(BASE).param("action", "search").param("query", "企画"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-autosearch=\"true\"")))
                .andExpect(content().string(containsString("contactIdFallback")))
                .andExpect(content().string(containsString("person-06")));
        mvc.perform(get(BASE).param("action", "choose").param("query", "企画")
                        .param("contactIdFallback", "person-06"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("渡辺 翔")))
                .andExpect(content().string(containsString("を選択しました。")));
    }

    @Test
    void selectedContactIdIsPreservedOnConfirmationPage() throws Exception {
        mvc.perform(get(BASE).param("action", "choose").param("contactId", "person-06"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"selected-contact-id\" type=\"hidden\" name=\"contactId\"")))
                .andExpect(content().string(containsString("value=\"person-06\"")))
                .andExpect(content().string(containsString("data-autosearch=\"false\"")));
    }

    @Test
    void missingAndUnknownCandidateIdsAreRejected() throws Exception {
        mvc.perform(get(BASE).param("action", "choose"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("担当者を候補から選択してください。")));
        mvc.perform(get(BASE).param("action", "choose").param("contactId", "person-999"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("候補を選び直してください。")));
    }

    @Test
    void oversizedCandidateIdStillProvidesMatchingCandidatesForReselection() throws Exception {
        mvc.perform(get(BASE).param("action", "choose").param("query", "企画")
                        .param("contactId", "x".repeat(41)))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("候補を選び直してください。")))
                .andExpect(content().string(containsString("data-autosearch=\"true\"")))
                .andExpect(content().string(containsString("contactIdFallback")))
                .andExpect(content().string(containsString("渡辺 翔")));
    }

    @Test
    void queryIsEscapedAndSelectionIsNotPersisted() throws Exception {
        mvc.perform(get(BASE).param("query", "<script>alert(1)</script>").param("action", "search"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("まだ担当者は確定していません。")));
    }
}
