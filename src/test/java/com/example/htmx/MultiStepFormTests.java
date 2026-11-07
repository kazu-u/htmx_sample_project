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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MultiStepFormTests {
    private static final String BASE = "/examples/multi-step-form";
    private static final String HTMX = "HX-Request";
    private static final String REVISION_PATTERN = "name=\"revision\"[^>]*value=\"([^\"]+)\"";

    @Autowired MockMvc mvc;

    @Test
    void showsTheFullPageAndOnlyTheWizardFragmentForHtmx() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(get(BASE).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("複数ステップフォーム")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("20分の無通信")))
                .andExpect(header().string("Vary", containsString(HTMX)));

        mvc.perform(get(BASE).session(session).header(HTMX, "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"application-wizard\"")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("<script"))));
    }

    @Test
    void applicantStepValidatesOnTheServerAndKeepsSubmittedValues() throws Exception {
        var session = new MockHttpSession();
        var revision = revisionFrom(mvc.perform(get(BASE).session(session)).andReturn());
        mvc.perform(post(BASE + "/applicant").session(session).with(csrf()).header(HTMX, "true")
                        .param("revision", revision).param("applicantName", " ").param("department", ""))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("申請者名を入力してください。")))
                .andExpect(content().string(containsString("部署を入力してください。")))
                .andExpect(content().string(containsString("id=\"applicant-name\"")))
                .andExpect(content().string(not(containsString("<h3>2. 出張内容</h3>"))));
    }

    @Test
    void invalidRegularFormsReturnFullPagesAndKeepSubmittedValues() throws Exception {
        var session = new MockHttpSession();
        var initialRevision = revisionFrom(mvc.perform(get(BASE).session(session)).andReturn());
        mvc.perform(post(BASE + "/applicant").session(session).with(csrf())
                        .param("revision", initialRevision).param("applicantName", "")
                        .param("department", "保持する部署"))
                .andExpect(status().is(422))
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("申請者名を入力してください。")))
                .andExpect(content().string(containsString("value=\"保持する部署\"")))
                .andExpect(content().string(not(containsString("<h3>2. 出張内容</h3>"))));

        mvc.perform(post(BASE + "/applicant").session(session).with(csrf())
                        .param("revision", initialRevision).param("applicantName", "通常フォーム確認")
                        .param("department", "総務部"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE));
        var stepTwoRevision = revisionFrom(mvc.perform(get(BASE).session(session)).andReturn());
        mvc.perform(post(BASE + "/travel").session(session).with(csrf())
                        .param("revision", stepTwoRevision).param("destination", "札幌")
                        .param("startDate", "2026-12-10").param("endDate", "2026-12-09")
                        .param("purpose", "現地確認"))
                .andExpect(status().is(422))
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("終了日は開始日以降を指定してください。")))
                .andExpect(content().string(containsString("value=\"2026-12-09\"")));

        var afterInvalidTravel = mvc.perform(get(BASE).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h3>2. 出張内容</h3>")))
                .andReturn();
        var confirmation = submitTravel(session, revisionFrom(afterInvalidTravel), "next", "札幌",
                "2026-12-10", "2026-12-11", "現地確認");
        assertThatContent(confirmation, "通常フォーム確認");
    }

    @Test
    void configuredTextLimitsAcceptTheBoundaryAndRejectOneMoreCharacter() throws Exception {
        var acceptedSession = new MockHttpSession();
        var initialRevision = revisionFrom(mvc.perform(get(BASE).session(acceptedSession)).andReturn());
        var stepTwoRevision = revisionFrom(submitApplicant(acceptedSession, initialRevision,
                "名".repeat(40), "部".repeat(40)));
        submitTravel(acceptedSession, stepTwoRevision, "next", "先".repeat(80),
                "2026-12-01", "2026-12-01", "目".repeat(200));

        assertApplicantRejected("名".repeat(41), "部署");
        assertApplicantRejected("申請者", "部".repeat(41));

        var longDestinationSession = new MockHttpSession();
        var longDestinationRevision = revisionFrom(mvc.perform(get(BASE).session(longDestinationSession)).andReturn());
        var longDestinationStepTwo = revisionFrom(submitApplicant(longDestinationSession,
                longDestinationRevision, "申請者", "部署"));
        mvc.perform(post(BASE + "/travel").session(longDestinationSession).with(csrf())
                        .header(HTMX, "true").param("revision", longDestinationStepTwo).param("action", "next")
                        .param("destination", "先".repeat(81)).param("startDate", "2026-12-01")
                        .param("endDate", "2026-12-02").param("purpose", "目的"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("行き先は80文字以内で入力してください。")));

        var longPurposeSession = new MockHttpSession();
        var longPurposeRevision = revisionFrom(mvc.perform(get(BASE).session(longPurposeSession)).andReturn());
        var longPurposeStepTwo = revisionFrom(submitApplicant(longPurposeSession,
                longPurposeRevision, "申請者", "部署"));
        mvc.perform(post(BASE + "/travel").session(longPurposeSession).with(csrf())
                        .header(HTMX, "true").param("revision", longPurposeStepTwo).param("action", "next")
                        .param("destination", "行き先").param("startDate", "2026-12-01")
                        .param("endDate", "2026-12-02").param("purpose", "目".repeat(201)))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("目的は200文字以内で入力してください。")));
    }

    @Test
    void travelStepCanReturnToApplicantWithoutLosingTravelDraft() throws Exception {
        var session = new MockHttpSession();
        var stepOneRevision = revisionFrom(mvc.perform(get(BASE).session(session)).andReturn());
        var stepTwo = submitApplicant(session, stepOneRevision, "青空 花子", "企画部");
        var stepTwoRevision = revisionFrom(stepTwo);

        var back = mvc.perform(post(BASE + "/travel").session(session).with(csrf()).header(HTMX, "true")
                        .param("revision", stepTwoRevision).param("action", "back")
                        .param("destination", "札幌").param("startDate", "2026-12-01")
                        .param("endDate", "2026-12-03").param("purpose", "展示会の視察"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h3>1. 申請者</h3>")))
                .andExpect(content().string(containsString("value=\"青空 花子\"")))
                .andReturn();

        var stepTwoAgain = submitApplicant(session, revisionFrom(back), "青空 花子", "企画部");
        assertThatContent(stepTwoAgain, "value=\"札幌\"");
        assertThatContent(stepTwoAgain, "value=\"2026-12-01\"");
        assertThatContent(stepTwoAgain, "展示会の視察");
    }

    @Test
    void travelStepRejectsInvalidDateRangeAndRetainsValues() throws Exception {
        var session = new MockHttpSession();
        var stepOneRevision = revisionFrom(mvc.perform(get(BASE).session(session)).andReturn());
        var stepTwoRevision = revisionFrom(submitApplicant(session, stepOneRevision, "若葉 太郎", "開発部"));

        mvc.perform(post(BASE + "/travel").session(session).with(csrf()).header(HTMX, "true")
                        .param("revision", stepTwoRevision).param("action", "next")
                        .param("destination", "大阪").param("startDate", "2026-12-10")
                        .param("endDate", "2026-12-09").param("purpose", "打ち合わせ"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("終了日は開始日以降を指定してください。")))
                .andExpect(content().string(containsString("value=\"2026-12-09\"")))
                .andExpect(content().string(not(containsString("<h3>3. 申請内容の確認</h3>"))));
    }

    @Test
    void reviewCanReturnToTravelAndCompleteOnlyOnce() throws Exception {
        var session = new MockHttpSession();
        var initialRevision = revisionFrom(mvc.perform(get(BASE).session(session)).andReturn());
        var stepTwoRevision = revisionFrom(submitApplicant(session, initialRevision, "<script>花子</script>", "企画部"));
        var confirmation = submitTravel(session, stepTwoRevision, "next", "京都", "2026-12-01",
                "2026-12-02", "研修");
        var confirmationRevision = revisionFrom(confirmation);

        mvc.perform(get(BASE).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;花子&lt;/script&gt;")))
                .andExpect(content().string(containsString("京都")));

        var returned = mvc.perform(post(BASE + "/back-to-travel").session(session).with(csrf())
                        .header(HTMX, "true").param("revision", confirmationRevision))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h3>2. 出張内容</h3>")))
                .andExpect(content().string(containsString("value=\"京都\"")))
                .andReturn();
        var secondConfirmation = submitTravel(session, revisionFrom(returned), "next", "京都",
                "2026-12-01", "2026-12-02", "研修");
        var currentRevision = revisionFrom(secondConfirmation);

        mvc.perform(post(BASE + "/complete").session(session).with(csrf()).header(HTMX, "true")
                        .param("revision", currentRevision))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("架空の申請を完了しました。")));
        mvc.perform(post(BASE + "/complete").session(session).with(csrf()).header(HTMX, "true")
                        .param("revision", currentRevision))
                .andExpect(status().isConflict());
    }

    @Test
    void resetClearsSessionDraftAndSessionsAreIsolated() throws Exception {
        var firstSession = new MockHttpSession();
        var secondSession = new MockHttpSession();
        var firstRevision = revisionFrom(mvc.perform(get(BASE).session(firstSession)).andReturn());
        var stepTwo = submitApplicant(firstSession, firstRevision, "朝日 葵", "営業部");
        var stepTwoRevision = revisionFrom(stepTwo);

        mvc.perform(get(BASE).session(secondSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"applicantName\"")))
                .andExpect(content().string(not(containsString("朝日 葵"))));
        mvc.perform(post(BASE + "/travel").session(secondSession).with(csrf()).header(HTMX, "true")
                        .param("revision", revisionFrom(mvc.perform(get(BASE).session(secondSession)).andReturn()))
                        .param("action", "next"))
                .andExpect(status().isConflict());
        mvc.perform(post(BASE + "/applicant").session(firstSession).with(csrf()).header(HTMX, "true")
                        .param("revision", firstRevision).param("applicantName", "古いタブ")
                        .param("department", "営業部"))
                .andExpect(status().isConflict());

        var reset = mvc.perform(post(BASE + "/reset").session(firstSession).with(csrf())
                        .header(HTMX, "true").param("revision", stepTwoRevision))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h3>1. 申請者</h3>")))
                .andExpect(content().string(not(containsString("朝日 葵"))))
                .andReturn();
        assertThatContent(reset, "入力内容を消去しました。");
    }

    @Test
    void regularFormsUsePostRedirectGetAndUpdatesRequireCsrf() throws Exception {
        var session = new MockHttpSession();
        var revision = revisionFrom(mvc.perform(get(BASE).session(session)).andReturn());
        mvc.perform(post(BASE + "/applicant").session(session).with(csrf())
                        .param("revision", revision).param("applicantName", "夕凪 蓮")
                        .param("department", "総務部"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE));
        mvc.perform(get(BASE).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h3>2. 出張内容</h3>")))
                .andExpect(content().string(containsString("申請者情報を確認しました。")));
        mvc.perform(post(BASE + "/reset").session(session).param("revision", revision))
                .andExpect(status().isForbidden());
    }

    private MvcResult submitApplicant(MockHttpSession session, String revision,
                                      String applicantName, String department) throws Exception {
        return mvc.perform(post(BASE + "/applicant").session(session).with(csrf()).header(HTMX, "true")
                        .param("revision", revision).param("applicantName", applicantName)
                        .param("department", department))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h3>2. 出張内容</h3>")))
                .andReturn();
    }

    private MvcResult submitTravel(MockHttpSession session, String revision, String action,
                                   String destination, String startDate, String endDate,
                                   String purpose) throws Exception {
        return mvc.perform(post(BASE + "/travel").session(session).with(csrf()).header(HTMX, "true")
                        .param("revision", revision).param("action", action)
                        .param("destination", destination).param("startDate", startDate)
                        .param("endDate", endDate).param("purpose", purpose))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h3>3. 申請内容の確認</h3>")))
                .andExpect(content().string(containsString("確認画面を表示しました。")))
                .andReturn();
    }

    private void assertApplicantRejected(String applicantName, String department) throws Exception {
        var session = new MockHttpSession();
        var revision = revisionFrom(mvc.perform(get(BASE).session(session)).andReturn());
        mvc.perform(post(BASE + "/applicant").session(session).with(csrf()).header(HTMX, "true")
                        .param("revision", revision).param("applicantName", applicantName)
                        .param("department", department))
                .andExpect(status().is(422));
    }

    private String revisionFrom(MvcResult result) throws Exception {
        Matcher matcher = Pattern.compile(REVISION_PATTERN).matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new AssertionError("revision hidden field was not rendered");
        }
        return matcher.group(1);
    }

    private void assertThatContent(MvcResult result, String expected) throws Exception {
        org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                .contains(expected);
    }
}
