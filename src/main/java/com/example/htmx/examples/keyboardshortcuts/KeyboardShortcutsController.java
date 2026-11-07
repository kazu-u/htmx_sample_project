package com.example.htmx.examples.keyboardshortcuts;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

/** キーボード操作とボタン操作で共通の固定パネルを取得する。 */
@Controller
@RequestMapping("/examples/keyboard-shortcuts")
public class KeyboardShortcutsController {
    private static final String PAGE = "examples/keyboard-shortcuts";
    private static final String RESULT_FRAGMENT = "examples/keyboard-shortcuts :: result";

    @GetMapping
    String page(Model model) {
        model.addAttribute("executed", false);
        return PAGE;
    }

    /** GET は固定の結果表示だけを返し、サーバー状態を変更しない。 */
    @GetMapping("/action")
    String action(@RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                  Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        model.addAttribute("executed", true);
        return htmx ? RESULT_FRAGMENT : PAGE;
    }
}
