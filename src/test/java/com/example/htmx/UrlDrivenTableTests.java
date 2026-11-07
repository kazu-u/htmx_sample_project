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

/** URL条件・固定データ・ページ全体と部分応答の一致を確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class UrlDrivenTableTests {
    private static final String BASE = "/examples/url-table";

    @Autowired
    MockMvc mvc;

    @Test
    void initialPageShowsLatestFiveOrdersAndSafeGetControls() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(header().string("Vary", containsString("HX-History-Restore-Request")))
                .andExpect(model().attribute("totalCount", 30))
                .andExpect(model().attribute("orders", org.hamcrest.Matchers.hasSize(5)))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("method=\"get\"")))
                .andExpect(content().string(containsString("hx-push-url=\"true\"")))
                .andExpect(content().string(containsString("name=\"keyword\"")))
                .andExpect(content().string(containsString("name=\"status\"")))
                .andExpect(content().string(containsString("name=\"sort\"")))
                .andExpect(content().string(containsString("ORD-2026-030")))
                .andExpect(content().string(not(containsString("ORD-2026-025"))))
                .andExpect(content().string(containsString("30件中 1〜5件")));
    }

    @Test
    void keywordStatusAndSortingAreCombinedBeforePagination() throws Exception {
        mvc.perform(get(BASE).param("keyword", "青空").param("status", "DELIVERED")
                        .param("sort", "number").param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalCount", 3))
                .andExpect(model().attribute("orders", org.hamcrest.Matchers.hasSize(3)))
                .andExpect(content().string(containsString("ORD-2026-001")))
                .andExpect(content().string(containsString("ORD-2026-013")))
                .andExpect(content().string(containsString("ORD-2026-025")))
                .andExpect(content().string(not(containsString("ORD-2026-007"))))
                .andExpect(content().string(containsString("name=\"keyword\" value=\"青空\"")))
                .andExpect(content().string(containsString("name=\"status\"")));
    }

    @Test
    void directPageUrlReproducesTheRequestedSliceAndKeepsAllConditions() throws Exception {
        mvc.perform(get(BASE).param("keyword", "ORD").param("status", "ALL")
                        .param("sort", "number").param("direction", "asc").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalCount", 30))
                .andExpect(model().attribute("orders", org.hamcrest.Matchers.hasSize(5)))
                .andExpect(content().string(containsString("ORD-2026-006")))
                .andExpect(content().string(containsString("ORD-2026-010")))
                .andExpect(content().string(not(containsString("ORD-2026-005"))))
                .andExpect(content().string(containsString("id=\"page-2\"")))
                .andExpect(content().string(containsString("name=\"page\" value=\"1\"")));
    }

    @Test
    void htmxRequestReturnsOnlyTheRegionWithStatefulLinks() throws Exception {
        mvc.perform(get(BASE).param("keyword", "花畑").param("sort", "customer")
                        .param("direction", "asc").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-History-Restore-Request")))
                .andExpect(content().string(containsString("id=\"order-region\"")))
                .andExpect(content().string(containsString("id=\"order-filter-form\"")))
                .andExpect(content().string(containsString("id=\"sort-customer\"")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("<script"))));
    }

    @Test
    void historyRestoreRequestReturnsTheWholePage() throws Exception {
        mvc.perform(get(BASE).param("status", "SHIPPED").param("page", "2")
                        .header("HX-Request", "true")
                        .header("HX-History-Restore-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("name=\"status\"")));
    }

    @Test
    void invalidKeywordStatusSortDirectionAndPageReturn422WithErrors() throws Exception {
        String maximumKeyword = "x".repeat(60);
        mvc.perform(get(BASE).param("keyword", maximumKeyword))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasNoErrors("query"))
                .andExpect(content().string(containsString("value=\"" + maximumKeyword + "\"")));
        mvc.perform(get(BASE).param("keyword", "x".repeat(61)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(model().attributeHasFieldErrors("query", "keyword"))
                .andExpect(content().string(containsString("検索語は60文字以内")))
                .andExpect(content().string(containsString("value=\"" + "x".repeat(61) + "\"")));
        mvc.perform(get(BASE).param("status", "UNKNOWN"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(model().attributeHasFieldErrors("query", "status"))
                .andExpect(content().string(containsString("配送状態を選び直してください。")));
        mvc.perform(get(BASE).param("sort", "unknown"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(model().attributeHasFieldErrors("query", "sort"));
        mvc.perform(get(BASE).param("direction", "sideways"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(model().attributeHasFieldErrors("query", "direction"));
        mvc.perform(get(BASE).param("page", "0"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(model().attributeHasFieldErrors("query", "page"))
                .andExpect(content().string(containsString("ページ番号は1以上")));
    }

    @Test
    void htmxValidationErrorReturnsSwappableRegionWithoutRows() throws Exception {
        mvc.perform(get(BASE).param("keyword", "x".repeat(61)).header("HX-Request", "true"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().string(containsString("id=\"order-region\"")))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")))
                .andExpect(content().string(not(containsString("ORD-2026-030"))))
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    void pageBeyondAvailableRangeIsNotFound() throws Exception {
        mvc.perform(get(BASE).param("page", "7")).andExpect(status().isNotFound());
        mvc.perform(get(BASE).param("keyword", "該当なし").param("page", "2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void queryTextIsEscapedAndSearchDoesNotDependOnSession() throws Exception {
        String query = "<script>alert(1)</script>";
        mvc.perform(get(BASE).param("keyword", query))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
        mvc.perform(get(BASE))
                .andExpect(model().attribute("totalCount", 30))
                .andExpect(content().string(containsString("name=\"keyword\" value=\"\"")));
    }
}
