package com.example.htmx.examples.infinitescroll;

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

/** 固定9件を3件ずつ返し、通常アクセスでは指定ページまでを表示する。 */
@Controller
@RequestMapping("/examples/infinite-scroll")
public class InfiniteScrollController {
    private static final int PAGE_SIZE = 3;
    private static final List<Book> BOOKS = List.of(
            new Book(1, "青空の散歩", "物語"),
            new Book(2, "若葉の図鑑", "自然"),
            new Book(3, "朝の台所", "暮らし"),
            new Book(4, "夕凪の便り", "物語"),
            new Book(5, "星の観察帳", "自然"),
            new Book(6, "小さな庭づくり", "暮らし"),
            new Book(7, "雲の旅", "物語"),
            new Book(8, "海辺の生き物", "自然"),
            new Book(9, "週末の工作", "暮らし"));
    private static final int PAGE_COUNT = (BOOKS.size() + PAGE_SIZE - 1) / PAGE_SIZE;

    @GetMapping
    String show(@RequestParam(defaultValue = "1") int page,
                @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                Model model, HttpServletResponse response) {
        if (page < 1 || page > PAGE_COUNT) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        int first = htmx ? (page - 1) * PAGE_SIZE : 0;
        model.addAttribute("books", BOOKS.subList(first, Math.min(page * PAGE_SIZE, BOOKS.size())));
        model.addAttribute("hasMore", page < PAGE_COUNT);
        model.addAttribute("nextPage", page + 1);
        response.addHeader("Vary", "HX-Request");
        return htmx ? "examples/infinite-scroll-rows :: pageRows" : "examples/infinite-scroll";
    }

    /** 実在しない書籍の表示専用データ。可変状態・DB・外部APIは使わない。 */
    public record Book(int id, String title, String category) {}
}
