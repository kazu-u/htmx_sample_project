package com.example.htmx.examples.inlinevalidation;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

/** メール欄の検証と送信時の全項目検証を行う。登録・メール送信・業務状態の保存は行わない。 */
@Controller
@RequestMapping("/examples/inline-validation")
public class InlineValidationController {
    @InitBinder("validationForm")
    void bindForm(WebDataBinder binder) {
        binder.setAllowedFields("name", "email");
    }

    @GetMapping
    String show(Model model, HttpServletResponse response) {
        model.addAttribute("validationForm", new ValidationForm());
        return render(false, false, model, response);
    }

    /** 補助検証は名前を検証せず、通常アクセスでは入力を含むページ全体を返す。 */
    @PostMapping("/email")
    String validateEmail(@Validated(ValidationForm.EmailCheck.class)
                         @ModelAttribute ValidationForm validationForm, BindingResult errors,
                         @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                         Model model, HttpServletResponse response) {
        if (errors.hasErrors()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
        }
        return render(htmx, true, model, response);
    }

    /** 過去の単独検証が成功していても、それを信用せず毎回全項目を検証する。 */
    @PostMapping
    String submit(@Valid @ModelAttribute ValidationForm validationForm, BindingResult errors,
                  @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                  Model model, HttpServletResponse response) {
        if (errors.hasErrors()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
        } else {
            model.addAttribute("notice", "全項目の検証に成功しました。登録やメール送信は行っていません。");
        }
        return render(htmx, false, model, response);
    }

    private String render(boolean htmx, boolean emailOnly, Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        model.addAttribute("emailChecked", true);
        if (!htmx) {
            return "examples/inline-validation";
        }
        return emailOnly ? "examples/inline-validation :: emailFeedback"
                : "examples/inline-validation :: validationPanel";
    }
}
