package com.example.htmx;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 有限ページ、断片と通常フォーム、終端、安全設定を実際のMVC経由で確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class InfiniteScrollTests {
    private static final String BASE = "/examples/infinite-scroll";
    @Autowired MockMvc mvc;

    @Test
    void initialPageHasAccessibleScrollRootAndManualAlternative() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("id=\"scroll-window\"")))
                .andExpect(content().string(containsString("intersect once root:#scroll-window, submit")))
                .andExpect(content().string(containsString("hx-sync=\"closest tbody:drop\"")))
                .andExpect(content().string(containsString("hx-disabled-elt=\"find button\"")))
                .andExpect(content().string(containsString("name=\"page\" value=\"2\"")))
                .andExpect(content().string(containsString("\"allowEval\":false")))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")));
    }

    @ParameterizedTest
    @CsvSource({"1,1,3", "2,4,6", "3,7,9"})
    void fragmentsContainExactlyOnePageAndEndHasNoTrigger(int page, int first, int last) throws Exception {
        String html = mvc.perform(get(BASE).param("page", Integer.toString(page)).header("HX-Request", "true"))
                .andExpect(status().isOk()).andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("<script"))))
                .andReturn().getResponse().getContentAsString();
        for (int id = 1; id <= 9; id++) {
            assertThat(html.contains("data-book-id=\"" + id + "\"")).isEqualTo(id >= first && id <= last);
        }
        assertThat(html.contains("id=\"scroll-more\"")).isEqualTo(page < 3);
        assertThat(html.contains("id=\"scroll-end\"")).isEqualTo(page == 3);
        if (page == 3) {
            assertThat(html).doesNotContain("hx-trigger", "hx-get", "name=\"page\"");
        }
    }

    @ParameterizedTest
    @CsvSource({"1,3", "2,6", "3,9"})
    void normalLinksShowAllRowsThroughRequestedPage(int page, int count) throws Exception {
        String html = mvc.perform(get(BASE).param("page", Integer.toString(page)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andReturn().getResponse().getContentAsString();
        assertThat(html.split("data-book-id=", -1).length - 1).isEqualTo(count);
    }

    @ParameterizedTest
    @CsvSource({"0,404", "4,404", "-1,404", "abc,400", "2147483648,400"})
    void invalidPagesAreRejectedForBothRepresentations(String page, int status) throws Exception {
        for (String htmx : java.util.List.of("true", "false")) {
            mvc.perform(get(BASE).param("page", page).header("HX-Request", htmx))
                    .andExpect(status().is(status));
        }
    }

    @Test
    void repeatedRequestsAndSessionsReturnTheSamePageWithoutBusinessState() throws Exception {
        var session = new MockHttpSession();
        String first = fragment(session);
        assertThat(fragment(session)).isEqualTo(first);
        assertThat(fragment(new MockHttpSession())).isEqualTo(first);
        assertThat(java.util.Collections.list(session.getAttributeNames()))
                .noneMatch(name -> name.startsWith("scopedTarget."));
    }

    private String fragment(MockHttpSession session) throws Exception {
        return mvc.perform(get(BASE).param("page", "2").header("HX-Request", "true").session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
