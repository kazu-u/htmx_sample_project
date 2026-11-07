package com.example.htmx.examples.tabshateoas;

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

/** URL ごとの選択状態から、タブ一覧と選択中の内容を一緒に返す。 */
@Controller
@RequestMapping("/examples/tabs-hateoas")
public class TabsHateoasController {
    private static final String PAGE = "examples/tabs-hateoas";
    private static final String TABS_FRAGMENT = "examples/tabs-hateoas-content :: tabContent";
    private static final List<Tab> TABS = List.of(
            new Tab("overview", "概要", "タブの選択状態は URL に含まれ、サーバーが HTML として返します。"),
            new Tab("mechanism", "仕組み", "各タブは同一オリジンの GET リンクです。htmx は応答で一覧と内容を差し替えます。"),
            new Tab("limits", "制約", "この画面は固定の架空説明だけを表示し、サーバー状態や利用者の入力は保存しません。")
    );

    @GetMapping
    String page(Model model) {
        addTabModel(model, TABS.getFirst());
        return PAGE;
    }

    @GetMapping("/tab/{tabId}")
    String select(@PathVariable String tabId,
                  @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                  @RequestHeader(name = "HX-History-Restore-Request", defaultValue = "false")
                  boolean historyRestore,
                  Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request, HX-History-Restore-Request");
        Tab selectedTab = TABS.stream()
                .filter(tab -> tab.id().equals(tabId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        addTabModel(model, selectedTab);
        return htmx && !historyRestore ? TABS_FRAGMENT : PAGE;
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
