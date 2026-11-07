package com.example.htmx.examples.confirm;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

/** 確認後に固定の結果を返し、htmx 要求と通常アクセスで応答形式を分ける。 */
@Controller
@RequestMapping("/examples/confirm")
public class ConfirmController {
    private static final String PAGE = "examples/confirm";
    private static final String RESULT_PAGE = "examples/confirm-result";
    private static final String RESULT_FRAGMENT = "examples/confirm-result-content :: result";

    @GetMapping
    String page() {
        return PAGE;
    }

    @GetMapping("/result")
    String result(@RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                  HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        return htmx ? RESULT_FRAGMENT : RESULT_PAGE;
    }
}
