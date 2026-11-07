package com.example.htmx.examples.animations;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/** 固定の表示候補を返し、CSS transition と htmx の差し替えを確認する。 */
@Controller
@RequestMapping("/examples/animations")
public class AnimationsController {
    private static final List<Appearance> APPEARANCES = List.of(
            new Appearance("coral", "珊瑚色", "落ち着いた珊瑚色のカードです。"),
            new Appearance("ocean", "海色", "深い青色のカードです。"),
            new Appearance("forest", "森林色", "落ち着いた緑色のカードです。"));

    @GetMapping
    String page(Model model) {
        model.addAttribute("appearance", APPEARANCES.getFirst());
        model.addAttribute("appearances", APPEARANCES);
        return "examples/animations";
    }

    /** htmx はカード断片、通常アクセスはカードを含むページ全体を返す。 */
    @GetMapping("/card")
    String card(@RequestParam String id,
                @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        Appearance appearance = APPEARANCES.stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("appearance", appearance);
        model.addAttribute("appearances", APPEARANCES);
        return htmx ? "examples/animations :: animationCard" : "examples/animations";
    }

    public record Appearance(String id, String name, String description) {}
}
