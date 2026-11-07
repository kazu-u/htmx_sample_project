package com.example.htmx.examples.tabsjavascript;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

/** 固定コンテンツを返し、選択状態の更新はブラウザー側に任せる。 */
@Controller
@RequestMapping("/examples/tabs-javascript")
public class TabsJavascriptController {
    private static final String PAGE = "examples/tabs-javascript";
    private static final String PANEL_FRAGMENT = "examples/tabs-javascript-panel :: panel";
    private static final List<Tab> TABS = List.of(
            new Tab("overview", "概要", "タブの選択状態はブラウザー側で更新し、サーバーは選ばれた内容を返します。"),
            new Tab("mechanism", "仕組み", "htmx が GET で取得した HTML を差し替え、JavaScript が選択表示を同期します。"),
            new Tab("limits", "制約", "htmx の選択は URL に残らず、ベースページを再読み込みすると概要に戻ります。")
    );

    @GetMapping
    String page(Model model) {
        addTabModel(model, TABS.getFirst());
        return PAGE;
    }

    @GetMapping("/tab/{tabId}")
    String select(@PathVariable String tabId,
                  @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                  Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        Tab selectedTab = TABS.stream()
                .filter(tab -> tab.id().equals(tabId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        addTabModel(model, selectedTab);
        return htmx ? PANEL_FRAGMENT : PAGE;
    }

    private void addTabModel(Model model, Tab selectedTab) {
        model.addAttribute("tabs", TABS);
        model.addAttribute("selectedTab", selectedTab);
    }

    public record Tab(String id, String label, String description) {
        public String heading() {
            return label();
        }
    }
}
