package com.example.htmx.examples.updatingothercontent;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 連絡先を追加し、主領域と OOB で更新する断片を選んで返す。 */
@Controller
@RequestMapping("/examples/updating-other-content")
public class UpdatingOtherContentController {
    private static final String PAGE = "examples/updating-other-content";
    private static final String RESPONSE = "examples/updating-other-content-response :: ";

    private final UpdatingOtherContentState state;

    public UpdatingOtherContentController(UpdatingOtherContentState state) {
        this.state = state;
    }

    @InitBinder("contactForm")
    void bindContact(WebDataBinder binder) {
        binder.setAllowedFields("name", "email", "revision");
    }

    @GetMapping
    String page(Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        UpdatingOtherContentState.Snapshot snapshot = state.snapshot();
        model.addAttribute("contactForm", formFor(snapshot.revision()));
        model.addAttribute("oobUpdate", false);
        addSnapshot(model, snapshot);
        return PAGE;
    }

    /** 入力が有効な場合だけ追加し、htmx では一覧・件数・通知を一緒に返す。 */
    @PostMapping
    String add(@Valid @ModelAttribute("contactForm") UpdatingContactForm contactForm,
               BindingResult errors,
               @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
               Model model, HttpServletResponse response, RedirectAttributes redirect) {
        response.addHeader("Vary", "HX-Request");
        model.addAttribute("oobUpdate", htmx);
        if (!hasValidRevisionFormat(contactForm.getRevision())) {
            errors.rejectValue("revision", "invalid", "フォームの版を確認できません。最新状態を再読み込みしてください。");
        }
        UpdatingOtherContentState.Snapshot currentSnapshot;
        if (errors.hasFieldErrors("revision")) {
            currentSnapshot = state.snapshot();
            model.addAttribute("revisionError", "フォームの版を確認できません。最新状態を再読み込みしてください。");
        } else {
            try {
                currentSnapshot = state.snapshotForRevision(contactForm.getRevision());
            } catch (ResponseStatusException exception) {
                if (exception.getStatusCode().value() != HttpStatus.CONFLICT.value()) {
                    throw exception;
                }
                return renderConflict(contactForm, htmx, model, response);
            }
        }
        if (errors.hasErrors()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
            addSnapshot(model, currentSnapshot);
            return htmx ? RESPONSE + "validation" : PAGE;
        }

        UpdatingOtherContentState.AddResult result;
        try {
            result = state.add(contactForm.getName(), contactForm.getEmail(), contactForm.getRevision());
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() != HttpStatus.CONFLICT.value()) {
                throw exception;
            }
            return renderConflict(contactForm, htmx, model, response);
        }
        if (!result.added()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
            model.addAttribute("capacityError", "登録上限の20件に達しました。初期化してから追加してください。");
            addSnapshot(model, result.snapshot());
            return htmx ? RESPONSE + "validation" : PAGE;
        }

        UpdatingOtherContentState.Snapshot snapshot = result.snapshot();
        if (!htmx) {
            redirect.addFlashAttribute("notice", "連絡先を追加しました。登録件数は" +
                    snapshot.contacts().size() + "件です。");
            return "redirect:/examples/updating-other-content";
        }

        model.addAttribute("contactForm", formFor(snapshot.revision()));
        model.addAttribute("notice", "連絡先を追加しました。登録件数は" +
                    snapshot.contacts().size() + "件です。");
        addSnapshot(model, snapshot);
        return RESPONSE + "success";
    }

    /** 初期化は CSRF 保護された POST とし、一覧・件数・通知を同時に更新する。 */
    @PostMapping("/reset")
    String reset(@RequestParam(name = "revision", required = false) String revision,
                 @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                 Model model, HttpServletResponse response, RedirectAttributes redirect) {
        response.addHeader("Vary", "HX-Request");
        if (!hasValidRevisionFormat(revision)) {
            response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
            UpdatingOtherContentState.Snapshot snapshot = state.snapshot();
            model.addAttribute("contactForm", formFor(snapshot.revision()));
            model.addAttribute("resetRevisionError",
                    "フォームの版を確認できません。最新状態を再読み込みしてください。");
            model.addAttribute("oobUpdate", htmx);
            addSnapshot(model, snapshot);
            return htmx ? RESPONSE + "validation" : PAGE;
        }

        UpdatingOtherContentState.Snapshot snapshot;
        try {
            snapshot = state.reset(revision);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() != HttpStatus.CONFLICT.value()) {
                throw exception;
            }
            return renderConflict(null, htmx, model, response);
        }
        if (!htmx) {
            redirect.addFlashAttribute("notice", "連絡先一覧を初期状態に戻しました。");
            return "redirect:/examples/updating-other-content";
        }

        model.addAttribute("notice", "連絡先一覧を初期状態に戻しました。");
        model.addAttribute("contactForm", formFor(snapshot.revision()));
        model.addAttribute("oobUpdate", true);
        addSnapshot(model, snapshot);
        return RESPONSE + "success";
    }

    private UpdatingContactForm formFor(long revision) {
        UpdatingContactForm form = new UpdatingContactForm();
        form.setRevision(Long.toString(revision));
        return form;
    }

    private String renderConflict(UpdatingContactForm contactForm, boolean htmx,
                                  Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.CONFLICT.value());
        UpdatingOtherContentState.Snapshot snapshot = state.snapshot();
        model.addAttribute("contactForm",
                contactForm == null ? formFor(snapshot.revision()) : contactForm);
        model.addAttribute("conflictError",
                "別の操作で更新されています。入力内容は保持しました。最新状態を再読み込みしてください。");
        model.addAttribute("oobUpdate", false);
        addSnapshot(model, snapshot);
        return htmx ? RESPONSE + "validation" : PAGE;
    }

    private boolean hasValidRevisionFormat(String revision) {
        if (revision == null) {
            return false;
        }
        try {
            long value = Long.parseLong(revision);
            return value > 0 && Long.toString(value).equals(revision);
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void addSnapshot(Model model, UpdatingOtherContentState.Snapshot snapshot) {
        model.addAttribute("contacts", snapshot.contacts());
        model.addAttribute("contactCount", snapshot.contacts().size());
        model.addAttribute("contactLimit", snapshot.limit());
        model.addAttribute("revision", Long.toString(snapshot.revision()));
    }
}
