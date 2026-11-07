package com.example.htmx;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 固定データの検索規則、入力境界、通常GETと結果断片の契約を確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class ActiveSearchTests {
    private static final String BASE = "/examples/active-search";
    @Autowired MockMvc mvc;

    @Test
    void initialPageIncludesAllEightContactsAndSafeSearchControls() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(model().attribute("contacts", hasSize(8)))
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("method=\"get\"")))
                .andExpect(content().string(containsString("hx-get=\"" + BASE + "\"")))
                .andExpect(content().string(containsString("hx-sync=\"this:replace\"")))
                .andExpect(content().string(containsString("hx-trigger=\"submit, active-search\"")))
                .andExpect(content().string(containsString("/js/active-search.js")))
                .andExpect(content().string(containsString("8件です。")))
                .andExpect(content().string(not(containsString("type=\"hidden\" name=\"_csrf\""))));
    }

    @ParameterizedTest
    @CsvSource({"佐藤,1,hanako.sato@example.com", "開発,1,riku.nakamura@example.com",
            "SATO,1,hanako.sato@example.com", "'  SATO  ',1,hanako.sato@example.com",
            "チーム,8,hanako.sato@example.com", "EXAMPLE.COM,8,riku.nakamura@example.com",
            "該当なし,0,該当する担当者はいません", "佐藤 開発,0,該当する担当者はいません"})
    void nameTeamAndEmailUseSingleCaseInsensitiveSubstring(String query, int count, String expected) throws Exception {
        for (String hx : new String[]{"true", "false"}) {
            mvc.perform(get(BASE).param("query", query).header("HX-Request", hx))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("contacts", hasSize(count)))
                    .andExpect(content().string(containsString(expected)))
                    .andExpect(content().string(containsString(count + "件です。")));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "　", "\t"})
    void blankQueryReturnsAllWithoutPersistingOrChangingOriginalInput(String query) throws Exception {
        mvc.perform(get(BASE).param("query", query).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("contacts", hasSize(8)))
                .andExpect(model().attributeHasNoErrors("searchForm"));
    }

    @ParameterizedTest
    @CsvSource({"59,200", "60,200", "61,422"})
    void lengthIsValidatedBeforeTrimmingAndSearch(int length, int expected) throws Exception {
        for (String query : new String[]{"字".repeat(length), " ".repeat(length)}) {
            for (String hx : new String[]{"true", "false"}) {
                var result = mvc.perform(get(BASE).param("query", query).header("HX-Request", hx))
                        .andExpect(status().is(expected));
                if (expected == 422) {
                    result.andExpect(model().attributeHasFieldErrors("searchForm", "query"))
                            .andExpect(content().string(containsString("検索語は60文字以内で入力してください。")))
                            .andExpect(content().string(containsString("data-search-error")))
                            .andExpect(content().string(not(containsString("id=\"search-count\""))));
                } else {
                    result.andExpect(model().attributeHasNoErrors("searchForm"));
                }
            }
        }
    }

    @Test
    void htmxOnlyReceivesResultRegionAndNeverReplacesInput() throws Exception {
        mvc.perform(get(BASE).param("query", "田中").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("id=\"search-results\"")))
                .andExpect(content().string(containsString("id=\"searched-query\">田中</span>")))
                .andExpect(content().string(not(containsString("<input"))))
                .andExpect(content().string(not(containsString("<script"))))
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    void invalidPlainQueryKeepsInputWhileHtmxGetsOnlyErrorRegion() throws Exception {
        String query = "長".repeat(61);
        mvc.perform(get(BASE).param("query", query))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("value=\"" + query + "\"")))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")));
        mvc.perform(get(BASE).param("query", query).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("id=\"search-error\"")))
                .andExpect(content().string(not(containsString("<input"))));
    }

    @Test
    void queryIsEscapedInInputAndResultWithoutHtmlExecution() throws Exception {
        String query = "<script>alert(1)</script>";
        for (String hx : new String[]{"true", "false"}) {
            mvc.perform(get(BASE).param("query", query).header("HX-Request", hx))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                    .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
        }
    }

    @Test
    void searchIsIndependentOfSessionAndDoesNotSaveLastQuery() throws Exception {
        var first = new MockHttpSession();
        mvc.perform(get(BASE).session(first).param("query", "佐藤"))
                .andExpect(model().attribute("contacts", hasSize(1)));
        mvc.perform(get(BASE).session(first))
                .andExpect(model().attribute("contacts", hasSize(8)))
                .andExpect(content().string(containsString("value=\"\"")));
        mvc.perform(get(BASE).session(new MockHttpSession()).param("query", "開発"))
                .andExpect(model().attribute("contacts", hasSize(1)));
    }

    @Test
    void matchingDoesNotDependOnServerDefaultLocale() throws Exception {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            mvc.perform(get(BASE).param("query", "ITO").header("HX-Request", "true"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("contacts", hasSize(1)))
                    .andExpect(content().string(containsString("aoi.ito@example.com")));
        } finally {
            Locale.setDefault(previous);
        }
    }
}
