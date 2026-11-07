package com.example.htmx.examples.webcomponents;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

/** Shadow DOM のカードから呼び出す固定データと、通常ページ表示を提供する。 */
@Controller
@RequestMapping("/examples/web-components")
public class WebComponentsController {
    private static final Map<String, Map<String, String>> SUMMARIES = Map.of(
            "inventory", Map.of(
                    "title", "在庫の概要",
                    "value", "書籍 24 冊",
                    "detail", "表示は学習用の固定データです。"),
            "orders", Map.of(
                    "title", "注文の概要",
                    "value", "本日の注文 12 件",
                    "detail", "注文処理は行っていません。"));

    @GetMapping
    String page() {
        return "examples/web-components";
    }

    @GetMapping("/summary/{topic}")
    String summary(@PathVariable String topic,
                   @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                   Model model,
                   HttpServletResponse response) {
        Map<String, String> summary = SUMMARIES.get(topic);
        if (summary == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        model.addAttribute("summary", summary);
        response.addHeader("Vary", "HX-Request");
        return htmx
                ? "examples/web-components-summary-content :: summary"
                : "examples/web-components-summary";
    }
}
