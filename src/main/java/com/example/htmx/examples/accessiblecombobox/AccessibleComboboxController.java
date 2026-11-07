package com.example.htmx.examples.accessiblecombobox;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

/** 固定の架空担当者から候補を絞り込み、送信されたIDを照合する。 */
@Controller
@RequestMapping("/examples/accessible-combobox")
public class AccessibleComboboxController {
    private static final int CANDIDATE_LIMIT = 10;
    private static final List<Contact> CONTACTS = List.of(
            new Contact("person-01", "佐藤 花子", "北日本チーム"),
            new Contact("person-02", "鈴木 太郎", "東日本チーム"),
            new Contact("person-03", "高橋 美咲", "中央チーム"),
            new Contact("person-04", "田中 健", "西日本チーム"),
            new Contact("person-05", "伊藤 葵", "南日本チーム"),
            new Contact("person-06", "渡辺 翔", "企画チーム"),
            new Contact("person-07", "山本 凛", "サポートチーム"),
            new Contact("person-08", "中村 陸", "開発チーム"),
            new Contact("person-09", "小林 彩", "広報チーム"),
            new Contact("person-10", "加藤 悠", "人事チーム"),
            new Contact("person-11", "吉田 結衣", "経理チーム"),
            new Contact("person-12", "山田 湊", "品質チーム")
    );

    @InitBinder("comboboxForm")
    void bindForm(WebDataBinder binder) {
        binder.setAllowedFields("query", "contactId", "contactIdFallback");
    }

    /** 通常GETでページ全体と選択結果を返す。選択IDは固定候補と照合する。 */
    @GetMapping
    String page(@Valid @ModelAttribute("comboboxForm") ComboboxForm form, BindingResult errors,
                @RequestParam(required = false) String action,
                Model model, HttpServletResponse response) {
        boolean invalidQuery = errors.hasFieldErrors("query");
        List<Contact> candidates = invalidQuery ? List.of() : findCandidates(form.getQuery());
        Contact selectedContact = null;
        String selectionError = null;

        if ("choose".equals(action)) {
            if (invalidQuery) {
                selectionError = "検索語を修正してから候補を選んでください。";
            } else {
                String selectedId = hasText(form.getContactId())
                        ? form.getContactId() : form.getContactIdFallback();
                selectedContact = findContact(selectedId);
                if (selectedContact == null) {
                    selectionError = hasText(selectedId)
                            ? "候補を選び直してください。" : "担当者を候補から選択してください。";
                }
            }
        }

        if (invalidQuery || selectionError != null) {
            response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
        }
        model.addAttribute("candidates", candidates);
        model.addAttribute("selectedContact", selectedContact);
        model.addAttribute("selectionError", selectionError);
        model.addAttribute("queryError", invalidQuery);
        model.addAttribute("candidateRegionHidden", true);
        model.addAttribute("autoSearchCandidates",
                selectedContact == null && !invalidQuery && hasText(form.getQuery()));
        return "examples/accessible-combobox";
    }

    /** 入力値を検証し、上限付き候補リストだけを返す。 */
    @GetMapping("/candidates")
    String candidates(@Valid @ModelAttribute("comboboxForm") ComboboxForm form,
                      BindingResult errors, Model model, HttpServletResponse response) {
        boolean invalidQuery = errors.hasFieldErrors("query");
        List<Contact> candidates = invalidQuery ? List.of() : findCandidates(form.getQuery());
        if (invalidQuery) {
            response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
        }
        model.addAttribute("candidates", candidates);
        model.addAttribute("queryError", invalidQuery);
        model.addAttribute("candidateRegionHidden", false);
        return "examples/accessible-combobox :: candidateRegion";
    }

    private List<Contact> findCandidates(String query) {
        if (!hasText(query)) {
            return List.of();
        }
        String normalizedQuery = query.strip().toLowerCase(Locale.ROOT);
        return CONTACTS.stream()
                .filter(contact -> contact.name().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || contact.department().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .limit(CANDIDATE_LIMIT)
                .toList();
    }

    private Contact findContact(String id) {
        if (!hasText(id)) {
            return null;
        }
        return CONTACTS.stream().filter(contact -> contact.id().equals(id)).findFirst().orElse(null);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record Contact(String id, String name, String department) {}
}
