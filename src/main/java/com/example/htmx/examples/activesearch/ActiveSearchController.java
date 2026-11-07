package com.example.htmx.examples.activesearch;

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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

/** 有限の架空データを検索し、通常ページまたは検索結果だけを返す。状態は保存しない。 */
@Controller
@RequestMapping("/examples/active-search")
public class ActiveSearchController {
    private static final List<Contact> CONTACTS = List.of(
            new Contact("佐藤 花子", "北日本チーム", "hanako.sato@example.com"),
            new Contact("鈴木 太郎", "東日本チーム", "taro.suzuki@example.com"),
            new Contact("高橋 美咲", "中央チーム", "misaki.takahashi@example.com"),
            new Contact("田中 健", "西日本チーム", "ken.tanaka@example.com"),
            new Contact("伊藤 葵", "南日本チーム", "aoi.ito@example.com"),
            new Contact("渡辺 翔", "企画チーム", "sho.watanabe@example.com"),
            new Contact("山本 凛", "サポートチーム", "rin.yamamoto@example.com"),
            new Contact("中村 陸", "開発チーム", "riku.nakamura@example.com")
    );

    @InitBinder("searchForm")
    void bindForm(WebDataBinder binder) {
        binder.setAllowedFields("query");
    }

    /** GETは参照のみ。空欄は全8件、不正入力は検索せず422で値とエラーを返す。 */
    @GetMapping
    String search(@Valid @ModelAttribute SearchForm searchForm, BindingResult errors,
                  @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                  Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        if (errors.hasErrors()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
            model.addAttribute("contacts", List.of());
        } else {
            String query = searchForm.getQuery().strip().toLowerCase(Locale.ROOT);
            model.addAttribute("contacts", CONTACTS.stream().filter(contact ->
                    contact.name().toLowerCase(Locale.ROOT).contains(query)
                    || contact.team().toLowerCase(Locale.ROOT).contains(query)
                    || contact.email().toLowerCase(Locale.ROOT).contains(query)).toList());
        }
        return htmx ? "examples/active-search :: searchResults" : "examples/active-search";
    }

    /** 検索対象の固定値。個人情報・外部API・共有の可変データは持たない。 */
    public record Contact(String name, String team, String email) {}
}
