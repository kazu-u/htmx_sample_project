package com.example.htmx.examples.undoabledelete;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 削除要求・期限内の取り消し・通常フォームを扱う。 */
@Controller
@RequestMapping("/examples/undoable-delete")
public class UndoableDeleteController {
    private final UndoableDeleteState state;

    public UndoableDeleteController(UndoableDeleteState state) {
        this.state = state;
    }

    @GetMapping
    String show(@RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        addTasks(model);
        return htmx ? "examples/undoable-delete :: taskList" : "examples/undoable-delete";
    }

    @PostMapping("/{id}/delete")
    String requestDeletion(@PathVariable int id, @RequestParam long revision,
                           @RequestParam(name = "confirmDelete", defaultValue = "false")
                           boolean confirmDelete,
                           @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                           Model model, HttpServletResponse response, RedirectAttributes redirect) {
        if (!htmx && !confirmDelete) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        var task = state.requestDeletion(id, revision, Instant.now());
        if (!htmx) {
            redirect.addFlashAttribute("notice", task.actionMessage());
            return "redirect:/examples/undoable-delete";
        }
        response.addHeader("Vary", "HX-Request");
        model.addAttribute("task", task);
        return "fragments/undoable-task-row :: taskRow";
    }

    @PostMapping("/{id}/undo")
    String undoDeletion(@PathVariable int id, @RequestParam long revision,
                        @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                        Model model, HttpServletResponse response, RedirectAttributes redirect) {
        var task = state.undoDeletion(id, revision, Instant.now());
        if (!htmx) {
            redirect.addFlashAttribute("notice", task.actionMessage());
            return "redirect:/examples/undoable-delete";
        }
        response.addHeader("Vary", "HX-Request");
        model.addAttribute("task", task);
        return "fragments/undoable-task-row :: taskRow";
    }

    @PostMapping("/reset")
    String reset(@RequestParam String revision,
                 @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                 Model model, HttpServletResponse response, RedirectAttributes redirect) {
        state.reset(revision, Instant.now());
        if (!htmx) {
            redirect.addFlashAttribute("notice", "一覧を初期状態に戻しました。");
            return "redirect:/examples/undoable-delete";
        }
        response.addHeader("Vary", "HX-Request");
        model.addAttribute("notice", "一覧を初期状態に戻しました。");
        addTasks(model);
        return "examples/undoable-delete :: taskList";
    }

    private void addTasks(Model model) {
        var snapshot = state.pageSnapshot(Instant.now());
        model.addAttribute("tasks", snapshot.tasks());
        model.addAttribute("listRevision", snapshot.revision());
    }
}
