package com.example.htmx.examples.progressbar;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 模擬ジョブの開始・キャンセルと、進捗のページまたは断片表示を扱う。 */
@Controller
@RequestMapping("/examples/progress-bar")
public class ProgressBarController {
    private final ProgressJobState state;

    public ProgressBarController(ProgressJobState state) {
        this.state = state;
    }

    @GetMapping
    String index(Model model) {
        return render(state.snapshot(Instant.now()), false, model, null);
    }

    /** 参照 GET は進捗を変更せず、実行中だけの進捗断片を返す。 */
    @GetMapping("/progress")
    String progress(@RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                    @RequestParam(defaultValue = "false") boolean includeControls,
                    Model model, HttpServletResponse response) {
        var snapshot = state.snapshot(Instant.now());
        response.addHeader("Vary", "HX-Request");
        model.addAttribute("job", snapshot);
        model.addAttribute("includeControls", includeControls);
        return htmx ? "examples/progress-state :: progressResponse" : "examples/progress-bar";
    }

    @PostMapping("/start")
    String start(@RequestParam ProgressJobState.Outcome outcome,
                 @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                 Model model, HttpServletResponse response, RedirectAttributes redirect) {
        var snapshot = state.start(outcome, Instant.now());
        if (!htmx) {
            redirect.addFlashAttribute("notice", "模擬ジョブを開始しました。");
            return "redirect:/examples/progress-bar";
        }
        return render(snapshot, true, model, response);
    }

    @PostMapping("/cancel")
    String cancel(@RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                  Model model, HttpServletResponse response, RedirectAttributes redirect) {
        var snapshot = state.cancel(Instant.now());
        if (!htmx) {
            redirect.addFlashAttribute("notice", "模擬ジョブをキャンセルしました。");
            return "redirect:/examples/progress-bar";
        }
        return render(snapshot, true, model, response);
    }

    private String render(ProgressJobState.Snapshot snapshot, boolean htmx,
                          Model model, HttpServletResponse response) {
        if (response != null) {
            response.addHeader("Vary", "HX-Request");
        }
        model.addAttribute("job", snapshot);
        return htmx ? "examples/progress-panel :: progressPanel" : "examples/progress-bar";
    }
}
