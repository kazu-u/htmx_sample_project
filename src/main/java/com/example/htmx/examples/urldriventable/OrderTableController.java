package com.example.htmx.examples.urldriventable;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
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
import org.springframework.web.server.ResponseStatusException;

/** 固定の注文データを URL の条件で絞り込み、全体ページか表の断片で返す。 */
@Controller
@RequestMapping("/examples/url-table")
public class OrderTableController {
    private static final int PAGE_SIZE = 5;
    private static final String PAGE = "examples/url-table";
    private static final List<Order> ORDERS = List.of(
            new Order("ORD-2026-001", "青空文具店", LocalDate.of(2026, 1, 8), "DELIVERED"),
            new Order("ORD-2026-002", "森の道具店", LocalDate.of(2026, 1, 10), "SHIPPED"),
            new Order("ORD-2026-003", "星の喫茶室", LocalDate.of(2026, 1, 12), "PREPARING"),
            new Order("ORD-2026-004", "波音ストア", LocalDate.of(2026, 1, 15), "NEW"),
            new Order("ORD-2026-005", "月見マーケット", LocalDate.of(2026, 1, 18), "DELIVERED"),
            new Order("ORD-2026-006", "花畑商会", LocalDate.of(2026, 1, 21), "SHIPPED"),
            new Order("ORD-2026-007", "青空文具店", LocalDate.of(2026, 1, 23), "PREPARING"),
            new Order("ORD-2026-008", "森の道具店", LocalDate.of(2026, 1, 25), "NEW"),
            new Order("ORD-2026-009", "星の喫茶室", LocalDate.of(2026, 1, 28), "DELIVERED"),
            new Order("ORD-2026-010", "波音ストア", LocalDate.of(2026, 2, 1), "SHIPPED"),
            new Order("ORD-2026-011", "月見マーケット", LocalDate.of(2026, 2, 3), "PREPARING"),
            new Order("ORD-2026-012", "花畑商会", LocalDate.of(2026, 2, 5), "NEW"),
            new Order("ORD-2026-013", "青空文具店", LocalDate.of(2026, 2, 8), "DELIVERED"),
            new Order("ORD-2026-014", "森の道具店", LocalDate.of(2026, 2, 10), "SHIPPED"),
            new Order("ORD-2026-015", "星の喫茶室", LocalDate.of(2026, 2, 12), "PREPARING"),
            new Order("ORD-2026-016", "波音ストア", LocalDate.of(2026, 2, 15), "NEW"),
            new Order("ORD-2026-017", "月見マーケット", LocalDate.of(2026, 2, 17), "DELIVERED"),
            new Order("ORD-2026-018", "花畑商会", LocalDate.of(2026, 2, 20), "SHIPPED"),
            new Order("ORD-2026-019", "青空文具店", LocalDate.of(2026, 2, 22), "PREPARING"),
            new Order("ORD-2026-020", "森の道具店", LocalDate.of(2026, 2, 24), "NEW"),
            new Order("ORD-2026-021", "星の喫茶室", LocalDate.of(2026, 2, 27), "DELIVERED"),
            new Order("ORD-2026-022", "波音ストア", LocalDate.of(2026, 3, 1), "SHIPPED"),
            new Order("ORD-2026-023", "月見マーケット", LocalDate.of(2026, 3, 4), "PREPARING"),
            new Order("ORD-2026-024", "花畑商会", LocalDate.of(2026, 3, 6), "NEW"),
            new Order("ORD-2026-025", "青空文具店", LocalDate.of(2026, 3, 9), "DELIVERED"),
            new Order("ORD-2026-026", "森の道具店", LocalDate.of(2026, 3, 12), "SHIPPED"),
            new Order("ORD-2026-027", "星の喫茶室", LocalDate.of(2026, 3, 15), "PREPARING"),
            new Order("ORD-2026-028", "波音ストア", LocalDate.of(2026, 3, 18), "NEW"),
            new Order("ORD-2026-029", "月見マーケット", LocalDate.of(2026, 3, 21), "DELIVERED"),
            new Order("ORD-2026-030", "花畑商会", LocalDate.of(2026, 3, 24), "SHIPPED")
    );
    private static final List<StatusOption> STATUSES = List.of(
            new StatusOption("ALL", "すべて"),
            new StatusOption("NEW", "受付"),
            new StatusOption("PREPARING", "準備中"),
            new StatusOption("SHIPPED", "発送済み"),
            new StatusOption("DELIVERED", "配達済み")
    );

    @InitBinder("query")
    void bindQuery(WebDataBinder binder) {
        binder.setAllowedFields("keyword", "status", "sort", "direction", "page");
    }

    /** GET は固定データの参照だけを行い、URL条件をそのまま再現できるページを返す。 */
    @GetMapping
    String show(@Valid @ModelAttribute("query") OrderTableQuery query, BindingResult errors,
                @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                @RequestHeader(name = "HX-History-Restore-Request", defaultValue = "false")
                boolean historyRestore,
                Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request, HX-History-Restore-Request");

        List<Order> matchingOrders = List.of();
        int pageCount = 1;
        if (errors.hasErrors()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
        } else {
            matchingOrders = findOrders(query);
            pageCount = Math.max(1, (matchingOrders.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (query.getPage() > pageCount) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }

        int firstIndex = errors.hasErrors() ? 0 : (query.getPage() - 1) * PAGE_SIZE;
        int lastIndex = Math.min(firstIndex + PAGE_SIZE, matchingOrders.size());
        List<Order> pageOrders = firstIndex < matchingOrders.size()
                ? matchingOrders.subList(firstIndex, lastIndex) : List.of();

        model.addAttribute("orders", pageOrders);
        model.addAttribute("statuses", STATUSES);
        model.addAttribute("totalCount", matchingOrders.size());
        model.addAttribute("pageCount", pageCount);
        model.addAttribute("pageNumbers", IntStream.rangeClosed(1, pageCount).boxed().toList());
        model.addAttribute("firstResult", pageOrders.isEmpty() ? 0 : firstIndex + 1);
        model.addAttribute("lastResult", lastIndex);
        model.addAttribute("nextDirection", "asc".equals(query.getDirection()) ? "desc" : "asc");
        return htmx && !historyRestore ? PAGE + " :: orderRegion" : PAGE;
    }

    private List<Order> findOrders(OrderTableQuery query) {
        String keyword = query.getKeyword().strip().toLowerCase(Locale.ROOT);
        Comparator<Order> comparator = switch (query.getSort()) {
            case "number" -> Comparator.comparing(Order::number);
            case "customer" -> Comparator.comparing(Order::customer);
            default -> Comparator.comparing(Order::date);
        };
        if (query.getDirection().equals("desc")) {
            comparator = comparator.reversed();
        }

        return ORDERS.stream()
                .filter(order -> query.getStatus().equals("ALL") || order.status().equals(query.getStatus()))
                .filter(order -> keyword.isEmpty()
                        || order.number().toLowerCase(Locale.ROOT).contains(keyword)
                        || order.customer().toLowerCase(Locale.ROOT).contains(keyword))
                .sorted(comparator)
                .toList();
    }

    public record Order(String number, String customer, LocalDate date, String status) {
        public String statusLabel() {
            return switch (status) {
                case "NEW" -> "受付";
                case "PREPARING" -> "準備中";
                case "SHIPPED" -> "発送済み";
                case "DELIVERED" -> "配達済み";
                default -> "不明";
            };
        }
    }

    public record StatusOption(String value, String label) {}
}
