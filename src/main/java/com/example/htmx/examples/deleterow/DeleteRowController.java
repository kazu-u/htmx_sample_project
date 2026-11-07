package com.example.htmx.examples.deleterow;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** htmx の DELETE と通常フォームの POST で、一覧の一行を削除する。 */
@Controller
@RequestMapping("/examples/delete-row")
public class DeleteRowController {
    private final DeleteRowState state;

    public DeleteRowController(DeleteRowState state) {
        this.state = state;
    }

    /** 一覧は全体または tbody 断片で返し、GET では状態を変更しない。 */
    @GetMapping
    String show(@RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        model.addAttribute("tasks", state.snapshot());
        return htmx ? "examples/delete-row :: taskRows" : "examples/delete-row";
    }

    /** htmx の削除要求には空の200応答を返し、対象行を画面から除去する。 */
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteWithHtmx(@PathVariable int id) {
        state.delete(id);
        return ResponseEntity.ok().build();
    }

    /** JavaScript が使えない場合は確認欄付きの通常フォームで削除する。 */
    @PostMapping("/{id}")
    String deleteWithForm(@PathVariable int id,
                          @RequestParam(name = "confirmDelete", defaultValue = "false")
                          boolean confirmDelete,
                          RedirectAttributes redirect) {
        if (!confirmDelete) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        state.delete(id);
        redirect.addFlashAttribute("notice", "行を削除しました。");
        return "redirect:/examples/delete-row";
    }

    /** セッション内の一覧だけを初期状態に戻す。 */
    @PostMapping("/reset")
    String reset(RedirectAttributes redirect) {
        state.reset();
        redirect.addFlashAttribute("notice", "一覧を初期状態に戻しました。");
        return "redirect:/examples/delete-row";
    }
}
