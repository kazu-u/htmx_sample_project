package com.example.htmx.examples.clicktoedit;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 通常のページと htmx 用の連絡先断片を、同じテンプレートから返す。 */
@Controller
@RequestMapping("/examples/click-to-edit")
public class ClickToEditController {
    private final ContactState state;

    public ClickToEditController(ContactState state) {
        this.state = state;
    }

    @InitBinder("contactForm")
    void bindContact(WebDataBinder binder) {
        binder.setAllowedFields("name", "email", "revision");
    }

    /** 初期表示・キャンセル。GET は保存済みデータを参照するだけ。 */
    @GetMapping({"", "/contact"})
    String show(@RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                Model model, HttpServletResponse response) {
        return render(htmx, false, model, response);
    }

    /** 保存済みデータを、入力用フォームへコピーする。 */
    @GetMapping("/contact/edit")
    String edit(@RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                Model model, HttpServletResponse response) {
        var contact = state.snapshot();
        var form = new ContactForm();
        form.setName(contact.name());
        form.setEmail(contact.email());
        form.setRevision(contact.revision());
        model.addAttribute("contactForm", form);
        return render(htmx, true, model, response);
    }

    /** 入力不正なら422で値を保持し、正常なら保存する。CSRF は Security が先に検証する。 */
    @PostMapping("/contact")
    String save(@Valid @ModelAttribute ContactForm contactForm, BindingResult errors,
                @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                Model model, HttpServletResponse response, RedirectAttributes redirect) {
        if (!errors.hasFieldErrors("revision")) {
            state.requireCurrentRevision(contactForm.getRevision());
        }
        if (errors.hasErrors()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
            return render(htmx, true, model, response);
        }
        state.save(contactForm);
        if (!htmx) {
            redirect.addFlashAttribute("notice", "保存しました。");
            return "redirect:/examples/click-to-edit";
        }
        model.addAttribute("notice", "保存しました。");
        return render(true, false, model, response);
    }

    /** 初期化も更新操作なので POST と CSRF・版の照合を必要とする。 */
    @PostMapping("/reset")
    String reset(@RequestParam String revision,
                 @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                 Model model, HttpServletResponse response, RedirectAttributes redirect) {
        state.reset(revision);
        if (!htmx) {
            redirect.addFlashAttribute("notice", "初期状態に戻しました。");
            return "redirect:/examples/click-to-edit";
        }
        model.addAttribute("notice", "初期状態に戻しました。");
        return render(true, false, model, response);
    }

    private String render(boolean htmx, boolean editing, Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        model.addAttribute("contact", state.snapshot());
        model.addAttribute("editing", editing);
        return htmx ? "examples/click-to-edit :: contact" : "examples/click-to-edit";
    }
}
