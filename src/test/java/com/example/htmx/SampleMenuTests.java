package com.example.htmx;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SampleMenuTests {
    private static final Pattern OPEN_GROUP =
            Pattern.compile("<details open=\"open\">\\s*<summary>([^<]+)</summary>");
    private static final Pattern SAMPLE_LINK = Pattern.compile("href=\"/examples/([^\"]+)\"");

    @Autowired MockMvc mvc;

    @Test
    void onlyTheCurrentSamplesGroupIsExpanded() throws Exception {
        List<MenuCase> cases = List.of(
                new MenuCase("click-to-load", "読み込み・検索"),
                new MenuCase("inline-validation", "入力・フォーム"),
                new MenuCase("click-to-edit", "行・データ操作"),
                new MenuCase("value-select", "画面・操作部品"),
                new MenuCase("animations", "画面連携・応用"));

        for (MenuCase menuCase : cases) {
            String html = mvc.perform(get("/examples/" + menuCase.sample()))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            Matcher expandedGroup = OPEN_GROUP.matcher(html);

            assertTrue(expandedGroup.find(), "現在のグループが展開されること: " + menuCase.sample());
            assertEquals(menuCase.group(), expandedGroup.group(1));
            assertFalse(expandedGroup.find(), "他のグループは初期状態で閉じること: " + menuCase.sample());
            assertTrue(html.contains("aria-current=\"page\""));
        }
    }

    @Test
    void allSampleLinksAppearInTheGroupedMenuExactlyOnce() throws Exception {
        String html = mvc.perform(get("/examples/click-to-edit"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        int menuStart = html.indexOf("<nav");
        int menuEnd = html.indexOf("</nav>", menuStart);
        assertTrue(menuStart >= 0 && menuEnd > menuStart);

        Matcher sampleLinks = SAMPLE_LINK.matcher(html.substring(menuStart, menuEnd));
        Set<String> samplePaths = new HashSet<>();
        int linkCount = 0;
        while (sampleLinks.find()) {
            samplePaths.add(sampleLinks.group(1));
            linkCount++;
        }
        assertEquals(29, linkCount);
        assertEquals(linkCount, samplePaths.size());
    }

    private record MenuCase(String sample, String group) {}
}
