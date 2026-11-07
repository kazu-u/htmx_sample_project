package com.example.htmx.examples.editrow;

import com.example.htmx.examples.clicktoedit.ContactForm;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 行の表示・編集・保存を受け付け、通常ページまたは一行のHTMLを返す。 */
@Controller
@RequestMapping("/examples/edit-row")
public class EditRowController {
    private final EditRowState state;

    public EditRowController(EditRowState state) {
        this.state = state;
    }

    /** 名前・メール・版の制約は「クリックで編集」と共通のフォームを再利用する。 */
    @InitBinder("contactForm")
    void bindContact(WebDataBinder binder) {
        binder.setAllowedFields("name", "email", "revision");
    }

    @GetMapping
    String index(Model model, HttpServletResponse response) {
        return render(false, null, false, model, response);
    }

    /** キャンセルでは保存済みの行を再取得するだけで、サーバー状態は変えない。 */
    @GetMapping("/{id}")
    String show(@PathVariable int id,
                @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                Model model, HttpServletResponse response) {
        return render(htmx, id, false, model, response);
    }

    @GetMapping("/{id}/edit")
    String edit(@PathVariable int id,
                @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                Model model, HttpServletResponse response) {
        var person = person(state.snapshot(), id);
        var form = new ContactForm();
        form.setName(person.name());
        form.setEmail(person.email());
        form.setRevision(person.revision());
        model.addAttribute("contactForm", form);
        return render(htmx, id, true, model, response);
    }

    /** 422では入力を保持する。古い版は検証エラーより先に409で拒否する。 */
    @PostMapping("/{id}")
    String save(@PathVariable int id, @Valid @ModelAttribute ContactForm contactForm,
                BindingResult errors,
                @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                Model model, HttpServletResponse response, RedirectAttributes redirect) {
        person(state.snapshot(), id);
        if (!errors.hasFieldErrors("revision")) {
            state.requireCurrentRevision(id, contactForm.getRevision());
        }
        if (errors.hasErrors()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
            return render(htmx, id, true, model, response);
        }
        state.save(id, contactForm);
        if (!htmx) {
            redirect.addFlashAttribute("notice", "行を保存しました。");
            return "redirect:/examples/edit-row";
        }
        model.addAttribute("rowNotice", "行を保存しました。");
        return render(true, id, false, model, response);
    }

    /** 初期化は通常POST。画面全体を再取得し、開いている編集内容も破棄する。 */
    @PostMapping("/reset")
    String reset(@RequestParam String revision, RedirectAttributes redirect) {
        state.reset(revision);
        redirect.addFlashAttribute("notice", "一覧を初期状態に戻しました。");
        return "redirect:/examples/edit-row";
    }

    private String render(boolean htmx, Integer id, boolean editing,
                          Model model, HttpServletResponse response) {
        var snapshot = state.snapshot();
        response.addHeader("Vary", "HX-Request");
        model.addAttribute("people", snapshot.people());
        model.addAttribute("listRevision", snapshot.revision());
        model.addAttribute("editingId", editing ? id : null);
        if (!model.containsAttribute("contactForm")) {
            model.addAttribute("contactForm", new ContactForm());
        }
        if (id != null) {
            model.addAttribute("person", person(snapshot, id));
        }
        return htmx ? "examples/edit-row-row :: row(person=${person})" : "examples/edit-row";
    }

    private static EditRowState.Person person(EditRowState.Snapshot snapshot, int id) {
        return snapshot.people().stream().filter(person -> person.id() == id).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
