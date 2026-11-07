package com.example.htmx;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 行単位の削除、CSRF、セッション分離、通常フォームの代替を確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
class DeleteRowTests {
    private static final String BASE = "/examples/delete-row";

    @Autowired
    MockMvc mvc;

    @Test
    void fullPageAndRowsFragmentExposeDeleteControlsAndLearningNavigation() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(get(BASE).session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("hx-confirm=\"この行を削除しますか？\"")))
                .andExpect(content().string(containsString("hx-target=\"closest tr\"")))
                .andExpect(content().string(containsString("hx-params=\"none\"")))
                .andExpect(content().string(containsString("hx-delete=\"/examples/delete-row/1\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("name=\"confirmDelete\"")))
                .andExpect(content().string(containsString("id=\"task-row-4\"")));

        mvc.perform(get(BASE).session(session).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(containsString("id=\"task-row-1\"")))
                .andExpect(content().string(not(containsString("id=\"task-list\""))));
    }

    @Test
    void deleteUsesEmptySuccessResponseAndIsolatesSessionState() throws Exception {
        var firstSession = new MockHttpSession();
        mvc.perform(delete(BASE + "/2").session(firstSession).with(csrf().asHeader())
                        .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mvc.perform(get(BASE).session(firstSession))
                .andExpect(content().string(not(containsString("入力フォームの整理"))))
                .andExpect(content().string(containsString("テスト項目の見直し")));
        mvc.perform(get(BASE).session(new MockHttpSession()))
                .andExpect(content().string(containsString("入力フォームの整理")));
    }

    @Test
    void allMutationsRejectMissingOrInvalidCsrfAndPreserveTheSessionList() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(delete(BASE + "/1").session(session).with(csrf()))
                .andExpect(status().isOk());

        mvc.perform(delete(BASE + "/2").session(session).header("HX-Request", "true"))
                .andExpect(status().isForbidden());
        mvc.perform(delete(BASE + "/3").session(session).with(csrf().useInvalidToken())
                        .header("HX-Request", "true"))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/2").session(session).param("confirmDelete", "true"))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/3").session(session).with(csrf().useInvalidToken())
                        .param("confirmDelete", "true"))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/reset").session(session))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/reset").session(session).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());

        mvc.perform(get(BASE).session(session))
                .andExpect(content().string(not(containsString("画面の表示確認"))))
                .andExpect(content().string(containsString("入力フォームの整理")))
                .andExpect(content().string(containsString("テスト項目の見直し")))
                .andExpect(content().string(containsString("説明文の更新")));
    }

    @Test
    void normalFormRequiresConfirmationAndResetRestoresTheSessionList() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(post(BASE + "/3").session(session).with(csrf()))
                .andExpect(status().isBadRequest());
        mvc.perform(get(BASE).session(session))
                .andExpect(content().string(containsString("テスト項目の見直し")));

        mvc.perform(post(BASE + "/3").session(session).with(csrf())
                        .param("confirmDelete", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE));
        mvc.perform(get(BASE).session(session))
                .andExpect(content().string(not(containsString("テスト項目の見直し"))));

        mvc.perform(post(BASE + "/reset").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE));
        mvc.perform(get(BASE).session(session))
                .andExpect(content().string(containsString("テスト項目の見直し")));
    }

    @Test
    void missingAndMalformedIdsFailWithoutChangingTheList() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(delete(BASE + "/99").session(session).with(csrf())
                        .header("HX-Request", "true"))
                .andExpect(status().isNotFound());
        mvc.perform(delete(BASE + "/invalid").session(session).with(csrf()))
                .andExpect(status().isBadRequest());
        mvc.perform(get(BASE).session(session))
                .andExpect(content().string(containsString("画面の表示確認")))
                .andExpect(content().string(containsString("説明文の更新")));
    }

    @Test
    void deletingTheFinalRowShowsAnEmptyState() throws Exception {
        var session = new MockHttpSession();
        for (int id = 1; id <= 4; id++) {
            mvc.perform(delete(BASE + "/" + id).session(session).with(csrf().asHeader())
                            .header("HX-Request", "true"))
                    .andExpect(status().isOk());
        }

        mvc.perform(get(BASE).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("id=\"task-row-"))))
                .andExpect(content().string(containsString("id=\"empty-list-message\"")))
                .andExpect(content().string(containsString("一覧に作業はありません。")));
    }
}
