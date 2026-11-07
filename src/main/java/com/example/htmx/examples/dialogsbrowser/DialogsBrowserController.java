package com.example.htmx.examples.dialogsbrowser;

import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

/** ブラウザー標準の確認・入力ダイアログから受け取った値を検証して表示する。 */
@Controller
@RequestMapping("/examples/dialogs-browser")
public class DialogsBrowserController {
    private static final String PAGE = "examples/dialogs-browser";
    private static final String RESPONSE = "examples/dialogs-browser-response :: ";
    private static final int MAX_MESSAGE_LENGTH = 100;

    @GetMapping
    String page(Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        model.addAttribute("message", "");
        model.addAttribute("confirmed", false);
        model.addAttribute("confirmationError", false);
        return PAGE;
    }

    /** htmx では HX-Prompt、通常フォームでは入力値と確認欄を検証する。 */
    @PostMapping
    String submit(@RequestParam(name = "message", required = false) String formMessage,
                  @RequestParam(name = "confirmed", required = false) String confirmed,
                  @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                  @RequestHeader(name = "HX-Prompt", required = false) String promptedMessage,
                  Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        String message = formMessage;
        String messageError = null;
        if (htmx && promptedMessage != null) {
            try {
                message = UriUtils.decode(promptedMessage, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException exception) {
                messageError = "入力を読み取れませんでした。もう一度入力してください。";
            }
        } else if (htmx) {
            message = null;
        }
        if (messageError == null) {
            messageError = validateMessage(message, htmx);
        }
        boolean confirmationAccepted = "true".equals(confirmed);
        boolean confirmationMissing = !htmx && !confirmationAccepted;
        if (messageError != null || confirmationMissing) {
            response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
            model.addAttribute("message", formMessage == null ? "" : formMessage);
            model.addAttribute("confirmed", !htmx && confirmationAccepted);
            if (htmx) {
                model.addAttribute("dialogError", messageError);
                return RESPONSE + "error";
            }
            model.addAttribute("messageError", messageError);
            model.addAttribute("confirmationError", confirmationMissing);
            return PAGE;
        }

        model.addAttribute("submittedMessage", message);
        model.addAttribute("confirmed", false);
        return htmx ? RESPONSE + "success" : PAGE;
    }

    private String validateMessage(String message, boolean htmx) {
        if (message == null || message.isBlank()) {
            return htmx ? "入力が空です。もう一度入力してください。" : "メッセージを入力してください。";
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            return "メッセージは100文字以内で入力してください。";
        }
        return null;
    }
}
