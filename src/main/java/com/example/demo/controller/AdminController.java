package com.example.demo.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.PrintOrder;
import com.example.demo.entity.StoredFile;
import com.example.demo.service.PrintOrderService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final PrintOrderService printOrderService;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("stats", printOrderService.getDashboardStats());
        return "admin_dashboard";
    }

    @GetMapping("/orders")
    public String viewOrders(@RequestParam(value = "filter", required = false) String filter,
                             @RequestParam(value = "search", required = false) String search,
                             Model model) {
        model.addAttribute("orders", printOrderService.getOrdersForAdmin(filter, search));
        model.addAttribute("filter", filter == null || filter.isBlank() ? "active" : filter);
        model.addAttribute("search", search);
        model.addAttribute("statuses", OrderStatus.values());
        return "admin_orders";
    }

    @PostMapping("/orders/{id}/advance")
    public String advanceStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            printOrderService.advanceStatus(id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/orders/{id}/delete")
    public String deleteOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            printOrderService.deleteOrder(id);
            redirectAttributes.addFlashAttribute("success", "Order deleted.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    /** Streams the order's PDF; inline for preview, attachment with ?dl=1. */
    @GetMapping("/orders/{id}/file")
    public ResponseEntity<byte[]> orderFile(@PathVariable Long id,
                                            @RequestParam(value = "dl", required = false) String dl) {
        PrintOrder order = printOrderService.getOrder(id);
        StoredFile file = order.getFile();
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        String disposition = (dl != null ? "attachment" : "inline")
                + "; filename=\"" + file.getFileName().replace("\"", "") + "\"";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(file.getData());
    }
}
