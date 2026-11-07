package com.example.htmx.examples.clicktoload;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/** 固定データをページ単位で返し、「続き」の要求は行と次の操作に置き換える。 */
@Controller
@RequestMapping("/examples/click-to-load")
public class ClickToLoadController {
    private static final int PAGE_SIZE = 3;
    private static final List<Agent> AGENTS = List.of(
            new Agent("佐藤 花子", "北日本チーム"),
            new Agent("鈴木 太郎", "東日本チーム"),
            new Agent("高橋 美咲", "中央チーム"),
            new Agent("田中 健", "西日本チーム"),
            new Agent("伊藤 葵", "南日本チーム"),
            new Agent("渡辺 翔", "企画チーム"),
            new Agent("山本 凛", "サポートチーム"),
            new Agent("中村 陸", "開発チーム")
    );
    private static final int PAGE_COUNT = (AGENTS.size() + PAGE_SIZE - 1) / PAGE_SIZE;

    /** 通常アクセスでは指定ページまでを表示し、htmx要求では次のページだけを返す。 */
    @GetMapping
    String show(@RequestParam(defaultValue = "1") int page,
                @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                Model model, HttpServletResponse response) {
        if (page < 1 || page > PAGE_COUNT) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        int firstIndex = htmx ? (page - 1) * PAGE_SIZE : 0;
        int lastIndex = Math.min(page * PAGE_SIZE, AGENTS.size());
        model.addAttribute("agents", AGENTS.subList(firstIndex, lastIndex));
        model.addAttribute("hasMore", page < PAGE_COUNT);
        model.addAttribute("nextPage", page + 1);
        response.addHeader("Vary", "HX-Request");
        return htmx ? "examples/click-to-load-rows :: pageRows" : "examples/click-to-load";
    }

    /** 画面表示専用の架空データ。DB・外部 API・利用者間共有の可変状態は持たない。 */
    public record Agent(String name, String team) {}
}
