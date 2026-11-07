package com.example.htmx.examples.lazyloading;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

/** 初期画面と後から取得する固定集計を分け、GETでは業務状態を変更しない。 */
@Controller
@RequestMapping("/examples/lazy-loading")
public class LazyLoadingController {
    private static final List<Summary> SUMMARIES = List.of(
            new Summary("企画", 6), new Summary("開発", 12), new Summary("確認", 4));

    @GetMapping
    String index(Model model) {
        model.addAttribute("loaded", false);
        return "examples/lazy-loading";
    }

    /** htmxでは集計の断片、通常リンクでは集計を含む全体ページを返す。 */
    @GetMapping("/summary")
    String summary(@RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                   Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        model.addAttribute("loaded", true);
        model.addAttribute("summaries", SUMMARIES);
        return htmx ? "examples/lazy-loading :: summary" : "examples/lazy-loading";
    }

    /** 固定3件の架空集計。件数は0〜20で、セッションに保存しない。 */
    public record Summary(String phase, int count) {}
}
