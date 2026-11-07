package com.example.htmx.examples.dialogscustom;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

/** htmx ではダイアログ用の案内断片を返し、通常アクセスではページ全体を返す。 */
@Controller
@RequestMapping("/examples/dialogs-custom")
public class DialogsCustomController {
    private static final String PAGE = "examples/dialogs-custom";
    private static final String DETAIL_PAGE = "examples/dialogs-custom-detail";
    private static final String DETAIL_FRAGMENT = "examples/dialogs-custom-content :: content";

    @GetMapping
    String page() {
        return PAGE;
    }

    @GetMapping("/detail")
    String detail(@RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                  HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        return htmx ? DETAIL_FRAGMENT : DETAIL_PAGE;
    }
}
