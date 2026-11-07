package com.example.htmx.examples.multistepform;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/examples/multi-step-form")
public class MultiStepFormController {
    private final TravelApplicationState state;
    private final SmartValidator validator;

    public MultiStepFormController(TravelApplicationState state, SmartValidator validator) {
        this.state = state;
        this.validator = validator;
    }

    @InitBinder("applicationForm")
    void bindApplicationForm(WebDataBinder binder) {
        binder.setAllowedFields("applicantName", "department", "destination", "startDate",
                "endDate", "purpose", "revision");
    }

    @GetMapping({"", "/"})
    String show(@RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                Model model, HttpServletResponse response) {
        var snapshot = state.snapshot();
        return render(htmx, snapshot, TravelApplicationForm.from(snapshot), model, response);
    }

    @PostMapping("/applicant")
    String submitApplicant(
            @org.springframework.validation.annotation.Validated(
                    TravelApplicationForm.ApplicantStep.class)
            @ModelAttribute("applicationForm") TravelApplicationForm form,
            BindingResult errors,
            @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
            Model model, HttpServletResponse response, RedirectAttributes redirect) {
        if (!errors.hasFieldErrors("revision")) {
            state.requireCurrentStep(1, form.getRevision());
        }
        if (errors.hasErrors()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
            return render(htmx, state.snapshot(), form, model, response);
        }
        state.advanceApplicant(form);
        return transition(htmx, "申請者情報を確認しました。", model, response, redirect);
    }

    @PostMapping("/travel")
    String submitTravel(
            @ModelAttribute("applicationForm") TravelApplicationForm form,
            BindingResult errors,
            @RequestParam(name = "action", defaultValue = "next") String action,
            @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
            Model model, HttpServletResponse response, RedirectAttributes redirect) {
        if (!action.equals("next") && !action.equals("back")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (!errors.hasFieldErrors("revision")) {
            state.requireCurrentStep(2, form.getRevision());
        }

        var group = action.equals("next")
                ? TravelApplicationForm.TravelStep.class : TravelApplicationForm.SharedFields.class;
        validator.validate(form, errors, group);
        if (action.equals("next") && form.getStartDate() != null && form.getEndDate() != null
                && form.getEndDate().isBefore(form.getStartDate())
                && !errors.hasFieldErrors("endDate")) {
            errors.rejectValue("endDate", "dateRange", "終了日は開始日以降を指定してください。");
        }
        if (errors.hasErrors()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
            return render(htmx, state.snapshot(), form, model, response);
        }

        if (action.equals("back")) {
            state.returnToApplicant(form);
            return transition(htmx, "出張内容を下書きに保持しました。", model, response, redirect);
        }
        state.advanceTravel(form);
        return transition(htmx, "確認画面を表示しました。", model, response, redirect);
    }

    @PostMapping("/back-to-travel")
    String returnToTravel(@RequestParam String revision,
                          @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                          Model model, HttpServletResponse response, RedirectAttributes redirect) {
        state.returnToTravel(revision);
        return transition(htmx, "出張内容を確認・修正できます。", model, response, redirect);
    }

    @PostMapping("/complete")
    String complete(@RequestParam String revision,
                    @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                    Model model, HttpServletResponse response, RedirectAttributes redirect) {
        state.complete(revision);
        return transition(htmx, "申請を完了しました。", model, response, redirect);
    }

    @PostMapping("/reset")
    String reset(@RequestParam String revision,
                 @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                 Model model, HttpServletResponse response, RedirectAttributes redirect) {
        state.reset(revision);
        return transition(htmx, "入力内容を消去しました。", model, response, redirect);
    }

    private String transition(boolean htmx, String notice, Model model, HttpServletResponse response,
                              RedirectAttributes redirect) {
        if (!htmx) {
            redirect.addFlashAttribute("notice", notice);
            return "redirect:/examples/multi-step-form";
        }
        var snapshot = state.snapshot();
        model.addAttribute("notice", notice);
        return render(true, snapshot, TravelApplicationForm.from(snapshot), model, response);
    }

    private String render(boolean htmx, TravelApplicationState.Snapshot snapshot,
                          TravelApplicationForm form, Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("applicationForm", form);
        model.addAttribute("completed", snapshot.completed());
        return htmx ? "examples/multi-step-form :: wizard" : "examples/multi-step-form";
    }
}
