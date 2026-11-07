package com.example.htmx.examples.dashboard;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

/** 架空のダッシュボード情報を時刻から作り、全体ページまたはカード断片を返す。 */
@Controller
@RequestMapping("/examples/dashboard")
public class DashboardController {
    private static final DateTimeFormatter UPDATED_AT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @GetMapping
    String page(Model model) {
        addSummaries(model, Instant.now());
        return "examples/dashboard";
    }

    @GetMapping("/summary/orders")
    String orders(@RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                  Model model, HttpServletResponse response) {
        Instant now = Instant.now();
        response.addHeader("Vary", "HX-Request");
        if (!htmx) {
            addSummaries(model, now);
            return "examples/dashboard";
        }
        model.addAttribute("orders", ordersAt(now));
        return "examples/dashboard-cards :: ordersContent";
    }

    @GetMapping("/summary/inventory")
    String inventory(@RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                     Model model, HttpServletResponse response) {
        Instant now = Instant.now();
        response.addHeader("Vary", "HX-Request");
        if (!htmx) {
            addSummaries(model, now);
            return "examples/dashboard";
        }
        model.addAttribute("inventory", inventoryAt(now));
        return "examples/dashboard-cards :: inventoryContent";
    }

    private void addSummaries(Model model, Instant now) {
        model.addAttribute("orders", ordersAt(now));
        model.addAttribute("inventory", inventoryAt(now));
    }

    private OrderSummary ordersAt(Instant now) {
        long period = now.getEpochSecond() / 30;
        int pending = 1 + (int) (period % 5);
        return new OrderSummary(
                28 + (int) (period % 13),
                pending,
                pending >= 4 ? "確認が必要" : "通常",
                UPDATED_AT.format(now));
    }

    private InventorySummary inventoryAt(Instant now) {
        long period = now.getEpochSecond() / 30;
        int lowStockItems = (int) (period % 4);
        return new InventorySummary(
                120 + (int) ((period % 37 * 3) % 37),
                lowStockItems,
                lowStockItems >= 2 ? "補充を確認" : "在庫は安定",
                UPDATED_AT.format(now));
    }

    public record OrderSummary(int received, int pending, String status, String updatedAt) {}

    public record InventorySummary(int availableUnits, int lowStockItems, String status, String updatedAt) {}
}
