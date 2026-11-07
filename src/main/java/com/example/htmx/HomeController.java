package com.example.htmx;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 最初のサンプルへ案内する。 */
@Controller
public class HomeController {
    @GetMapping("/")
    String home() {
        return "redirect:/examples/click-to-edit";
    }
}
