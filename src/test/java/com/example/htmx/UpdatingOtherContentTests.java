package com.example.htmx;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UpdatingOtherContentTests {
    private static final String BASE = "/examples/updating-other-content";

    @Autowired MockMvc mvc;

    @Test
    void pageProvidesContactFormAndOutOfBandTargets() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get(BASE).session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("hx-target=\"#contacts-panel\"")))
                .andExpect(content().string(containsString("登録件数: 0 / 20")))
                .andExpect(content().string(containsString("aria-current=\"page\"")));
    }

    @Test
    void validHtmxSubmissionUpdatesTableCountAndStatus() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MvcResult result = submitHtmx(session, "山田 花子", "hanako@example.com", revisionFrom(session));
        org.springframework.test.util.AssertionErrors.assertEquals("success status", 200,
                result.getResponse().getStatus());
        org.springframework.test.util.AssertionErrors.assertTrue("expanded primary target",
                result.getResponse().getContentAsString().contains("id=\"contacts-panel\""));
        org.springframework.test.util.AssertionErrors.assertTrue("reset form is refreshed",
                result.getResponse().getContentAsString().contains("id=\"contacts-reset-form\""));
        mvc.perform(get(BASE).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("登録件数: 1 / 20")))
                .andExpect(content().string(containsString("山田 花子")));
        org.springframework.test.util.AssertionErrors.assertTrue("OOB count and status",
                result.getResponse().getContentAsString().contains("hx-swap-oob=\"outerHTML\""));
        org.springframework.test.util.AssertionErrors.assertTrue("partial response",
                !result.getResponse().getContentAsString().contains("<html"));
    }

    @Test
    void invalidHtmxSubmissionReturnsEscapedFormAndPreservesValues() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String maliciousName = "<script>" + "x".repeat(60) + "</script>";
        mvc.perform(post(BASE).session(session).param("name", maliciousName).param("email", "invalid")
                        .param("revision", revisionFrom(session)).with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(header().string("Vary", containsString("HX-Request")))
                .andExpect(content().string(containsString("id=\"contacts-panel\"")))
                .andExpect(content().string(containsString("&lt;script&gt;" + "x".repeat(60) + "&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>"))))
                .andExpect(content().string(containsString("名前は60文字以内")))
                .andExpect(content().string(containsString("メールアドレスの形式を確認してください。")))
                .andExpect(content().string(not(containsString("登録件数: 1 / 20"))));
    }

    @Test
    void invalidNormalSubmissionReturnsFullPageAndPreservesValues() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post(BASE).session(session).param("name", "  ").param("email", "not-an-email")
                        .param("revision", revisionFrom(session)).with(csrf()))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("名前を入力してください。")))
                .andExpect(content().string(containsString("メールアドレスの形式を確認してください。")));
    }

    @Test
    void missingMalformedOrOverlongRevisionShowsReloadInstruction() throws Exception {
        for (String invalidRevision : new String[]{"", "x", "x".repeat(37)}) {
            mvc.perform(post(BASE).param("name", "有効な名前").param("email", "valid@example.com")
                            .param("revision", invalidRevision).with(csrf()).header("HX-Request", "true"))
                    .andExpect(status().is(422))
                    .andExpect(content().string(containsString("フォームの版を確認できません。")))
                    .andExpect(content().string(containsString("最新状態を再読み込み")));
            mvc.perform(post(BASE + "/reset").param("revision", invalidRevision)
                            .with(csrf()).header("HX-Request", "true"))
                    .andExpect(status().is(422))
                    .andExpect(content().string(containsString("フォームの版を確認できません。")))
                    .andExpect(content().string(containsString("最新状態を再読み込み")));
        }
    }

    @Test
    void validNormalSubmissionUsesPostRedirectGet() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post(BASE).session(session).param("name", "佐藤 太郎")
                        .param("email", "taro@example.com").param("revision", revisionFrom(session)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE));

        mvc.perform(get(BASE).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("佐藤 太郎")))
                .andExpect(content().string(containsString("連絡先を追加しました。登録件数は1件です。")));
    }

    @Test
    void resetUsesCsrfAndUpdatesTheTableAndCount() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post(BASE).session(session).param("name", "追加連絡先")
                        .param("email", "added@example.com").param("revision", revisionFrom(session)).with(csrf()))
                .andExpect(status().is3xxRedirection());

        String revision = revisionFrom(session);
        mvc.perform(post(BASE + "/reset").session(session).param("revision", revision)
                        .with(csrf()).header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("登録件数: 0 / 20")))
                .andExpect(content().string(containsString("まだ連絡先はありません。")))
                .andExpect(content().string(containsString("連絡先一覧を初期状態に戻しました。")))
                .andExpect(content().string(not(containsString("追加連絡先"))));
    }

    @Test
    void bothMutationsRequireCsrf() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String revision = revisionFrom(session);
        mvc.perform(post(BASE).session(session).param("name", "CSRFなし")
                        .param("email", "csrf@example.com").param("revision", revision))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/reset").session(session).param("revision", revision))
                .andExpect(status().isForbidden());
    }

    @Test
    void contactListsAreIsolatedBySession() throws Exception {
        MockHttpSession firstSession = new MockHttpSession();
        MockHttpSession secondSession = new MockHttpSession();
        String firstRevision = revisionFrom(firstSession);
        mvc.perform(post(BASE).session(firstSession).param("name", "セッションA")
                        .param("email", "a@example.com").param("revision", firstRevision).with(csrf()))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get(BASE).session(firstSession))
                .andExpect(content().string(containsString("セッションA")))
                .andExpect(content().string(containsString("登録件数: 1 / 20")));
        mvc.perform(get(BASE).session(secondSession))
                .andExpect(content().string(not(containsString("セッションA"))))
                .andExpect(content().string(containsString("登録件数: 0 / 20")));
    }

    @Test
    void enforcesTheTwentyContactLimitWithoutPartialMutation() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String revision = revisionFrom(session);
        for (int index = 1; index <= 20; index++) {
            MvcResult result = submitHtmx(session, "連絡先" + index,
                    "contact" + index + "@example.com", revision);
            org.springframework.test.util.AssertionErrors.assertEquals("added contact " + index, 200,
                    result.getResponse().getStatus());
            revision = revisionFrom(result.getResponse().getContentAsString());
        }

        mvc.perform(post(BASE).session(session).param("name", "上限超過")
                        .param("email", "overflow@example.com").param("revision", revision)
                        .with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("登録上限の20件に達しました。")))
                .andExpect(content().string(containsString("contact-table-body")));

        mvc.perform(get(BASE).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("登録件数: 20 / 20")))
                .andExpect(content().string(not(containsString("overflow@example.com"))));
    }

    @Test
    void staleFormRevisionIsRejectedWithoutAddingAnotherContact() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String oldRevision = revisionFrom(session);
        mvc.perform(post(BASE).session(session).param("name", "先に追加")
                        .param("email", "first@example.com").param("revision", oldRevision)
                        .with(csrf()).header("HX-Request", "true"))
                .andExpect(status().isOk());

        mvc.perform(post(BASE).session(session).param("name", "古いフォーム")
                        .param("email", "stale@example.com").param("revision", oldRevision)
                        .with(csrf()).header("HX-Request", "true"))
                .andExpect(status().isConflict());
        mvc.perform(post(BASE + "/reset").session(session).param("revision", oldRevision)
                        .with(csrf()).header("HX-Request", "true"))
                .andExpect(status().isConflict());
        mvc.perform(post(BASE).session(session).param("name", "版不正でも最新一覧")
                        .param("email", "invalid-revision@example.com").param("revision", "x")
                        .with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("先に追加")))
                .andExpect(content().string(containsString("登録件数: 1 / 20")))
                .andExpect(content().string(containsString("hx-swap-oob=\"outerHTML\"")));
        mvc.perform(post(BASE).session(session).param("name", "JavaScriptなしの古いフォーム")
                        .param("email", "stale-no-js@example.com").param("revision", oldRevision)
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("id=\"revision-conflict-error\"")))
                .andExpect(content().string(containsString("JavaScriptなしの古いフォーム")))
                .andExpect(content().string(containsString("stale-no-js@example.com")))
                .andExpect(content().string(containsString("href=\"" + BASE + "\"")));
        mvc.perform(post(BASE + "/reset").session(session).param("revision", oldRevision).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("href=\"" + BASE + "\"")));
        mvc.perform(get(BASE).session(session))
                .andExpect(content().string(containsString("登録件数: 1 / 20")))
                .andExpect(content().string(not(containsString("stale@example.com"))));
    }

    @Test
    void acceptsNameLengthBoundariesAndRejectsAnOverlongName() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String revision = revisionFrom(session);
        for (int length : new int[]{59, 60}) {
            MvcResult result = submitHtmx(session, "あ".repeat(length), "valid@example.com", revision);
            org.springframework.test.util.AssertionErrors.assertEquals("name boundary " + length, 200,
                    result.getResponse().getStatus());
            revision = revisionFrom(result.getResponse().getContentAsString());
        }

        mvc.perform(post(BASE).session(session).param("name", "あ".repeat(61))
                        .param("email", "valid@example.com").param("revision", revision)
                        .with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("名前は60文字以内")));
    }

    @Test
    void acceptsEmailLengthBoundariesAndRejectsAnOverlongEmail() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String revision = revisionFrom(session);
        for (int length : new int[]{253, 254}) {
            MvcResult result = submitHtmx(session, "有効な名前", emailWithLength(length), revision);
            org.springframework.test.util.AssertionErrors.assertEquals("email boundary " + length, 200,
                    result.getResponse().getStatus());
            revision = revisionFrom(result.getResponse().getContentAsString());
        }

        mvc.perform(post(BASE).session(session).param("name", "有効な名前")
                        .param("email", emailWithLength(255)).param("revision", revision)
                        .with(csrf()).header("HX-Request", "true"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("メールアドレスは254文字以内")));
    }

    private MvcResult submitHtmx(MockHttpSession session, String name, String email, String revision)
            throws Exception {
        return mvc.perform(post(BASE).session(session).param("name", name).param("email", email)
                        .param("revision", revision).with(csrf()).header("HX-Request", "true"))
                .andReturn();
    }

    private String revisionFrom(MockHttpSession session) throws Exception {
        String html = mvc.perform(get(BASE).session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return revisionFrom(html);
    }

    private String revisionFrom(String html) {
        Matcher matcher = Pattern.compile("name=\"revision\" value=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find(), "revision hidden field is rendered");
        return matcher.group(1);
    }

    private String emailWithLength(int length) {
        String domain = "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
        return "a".repeat(length - domain.length() - 1) + "@" + domain;
    }
}
