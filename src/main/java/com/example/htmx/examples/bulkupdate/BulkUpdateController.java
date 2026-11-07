package com.example.htmx.examples.bulkupdate;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashSet;
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

/** 全行のチェック状態を一括保存し、ページ全体またはフォーム領域を返す。 */
@Controller
@RequestMapping("/examples/bulk-update")
public class BulkUpdateController {
    private final BulkUpdateState state;

    public BulkUpdateController(BulkUpdateState state) {
        this.state = state;
    }

    @InitBinder("bulkUpdateForm")
    void bind(WebDataBinder binder) {
        binder.setAllowedFields("activeIds", "revision");
        binder.setAutoGrowCollectionLimit(4);
        binder.setFieldDefaultPrefix(null);
        binder.setFieldMarkerPrefix(null);
    }

    @GetMapping
    String index(@RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                 Model model, HttpServletResponse response) {
        var snapshot = state.snapshot();
        model.addAttribute("bulkUpdateForm", form(snapshot));
        return render(snapshot, htmx, model, response);
    }

    /** 無効入力は全行を変更せず422、古い版は入力エラーより先に409にする。 */
    @PostMapping
    String save(@Valid @ModelAttribute BulkUpdateForm bulkUpdateForm, BindingResult errors,
                @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                Model model, HttpServletResponse response, HttpServletRequest request,
                RedirectAttributes redirect) {
        if (!errors.hasFieldErrors("revision")) {
            state.requireCurrentRevision(bulkUpdateForm.getRevision());
        }
        var ids = bulkUpdateForm.getActiveIds();
        if (request.getParameterMap().keySet().stream()
                .anyMatch(name -> !java.util.Set.of("activeIds", "revision", "_csrf").contains(name))) {
            errors.reject("unexpectedFields", "送信項目が不正です。再読み込みしてください。");
        }
        // 単独の空値が空リストへ変換されても、未送信による全解除と区別する。
        var submittedIds = request.getParameterValues("activeIds");
        if (submittedIds != null && java.util.Arrays.stream(submittedIds)
                .anyMatch(id -> !id.matches("[1-4]"))) {
            errors.reject("invalidIds", "対象の行を確認してください。");
        }
        if (ids != null && new HashSet<>(ids).size() != ids.size()) {
            errors.reject("duplicateIds", "同じ行が重複しています。選択を確認してください。");
        }
        if (ids == null) {
            errors.reject("missingIds", "選択情報が不正です。再読み込みしてください。");
        }
        if (errors.hasErrors()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
            return render(state.snapshot(), htmx, model, response);
        }
        var snapshot = state.save(new HashSet<>(ids), bulkUpdateForm.getRevision());
        if (!htmx) {
            redirect.addFlashAttribute("notice", "全4行の有効・無効を保存しました。");
            return "redirect:/examples/bulk-update";
        }
        model.asMap().remove(BindingResult.MODEL_KEY_PREFIX + "bulkUpdateForm");
        model.addAttribute("bulkUpdateForm", form(snapshot));
        model.addAttribute("notice", "全4行の有効・無効を保存しました。");
        return render(snapshot, true, model, response);
    }

    @PostMapping("/reset")
    String reset(@RequestParam String revision, RedirectAttributes redirect) {
        state.reset(revision);
        redirect.addFlashAttribute("notice", "一覧を初期状態に戻しました。");
        return "redirect:/examples/bulk-update";
    }

    private String render(BulkUpdateState.Snapshot snapshot, boolean htmx,
                          Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        model.addAttribute("people", snapshot.people());
        model.addAttribute("listRevision", snapshot.revision());
        return htmx ? "examples/bulk-update :: panel" : "examples/bulk-update";
    }

    private static BulkUpdateForm form(BulkUpdateState.Snapshot snapshot) {
        var form = new BulkUpdateForm();
        form.setRevision(snapshot.revision());
        form.setActiveIds(snapshot.people().stream().filter(BulkUpdateState.Person::active)
                .map(person -> Integer.toString(person.id())).toList());
        return form;
    }
}
