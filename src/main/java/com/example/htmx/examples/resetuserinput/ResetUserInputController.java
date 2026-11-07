package com.example.htmx.examples.resetuserinput;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

/** 入力検証に成功した応答だけが、ブラウザーのフォーム初期化を許可する例。 */
@Controller
@RequestMapping("/examples/reset-user-input")
public class ResetUserInputController {
    private static final String PAGE = "examples/reset-user-input";
    private static final String RESPONSE = "examples/reset-user-input-response :: ";

    @GetMapping
    String page(Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        model.addAttribute("noteForm", new NoteForm());
        return PAGE;
    }

    @PostMapping
    String submit(@Valid @ModelAttribute("noteForm") NoteForm noteForm, BindingResult errors,
                  @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                  Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        if (errors.hasErrors()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
            model.addAttribute("noteError", errors.getFieldError("text").getDefaultMessage());
            return htmx ? RESPONSE + "validationError" : PAGE;
        }

        model.addAttribute("submittedNote", noteForm.getText());
        model.addAttribute("noteForm", new NoteForm());
        return htmx ? RESPONSE + "noteItem" : PAGE;
    }
}
