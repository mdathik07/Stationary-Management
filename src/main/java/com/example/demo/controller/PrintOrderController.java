package com.example.demo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.config.PricingProperties;
import com.example.demo.entity.PrintOrder;
import com.example.demo.entity.PrintType;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.PrintOrderService;
import com.example.demo.service.impl.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/student")
public class PrintOrderController {

    private final PrintOrderService printOrderService;
    private final UserRepository userRepository;
    private final PricingProperties pricing;

    @GetMapping("/dashboard")
    public String studentDashboard() {
        return "student_dashboard";
    }

    @GetMapping("/upload-order")
    public String showUploadForm(Model model) {
        model.addAttribute("pricing", pricing);
        return "upload_order";
    }

    @PostMapping("/upload-order")
    public String handleUpload(@RequestParam("file") MultipartFile file,
                               @RequestParam("printType") PrintType printType,
                               @RequestParam(value = "doubleSided", defaultValue = "false") boolean doubleSided,
                               @RequestParam("copies") int copies,
                               @RequestParam(value = "notes", required = false) String notes,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        User user = currentUser(userDetails);
        try {
            PrintOrder order = printOrderService.createOrder(user, file, printType, doubleSided, copies, notes);
            return "redirect:/student/order-success/" + order.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/upload-order";
        }
    }

    @GetMapping("/order-success/{id}")
    public String orderSuccess(@PathVariable Long id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model) {
        PrintOrder order = printOrderService.getOrder(id);
        if (order.getUser() == null || !order.getUser().getId().equals(userDetails.getUser().getId())) {
            return "redirect:/student/my-orders";
        }
        model.addAttribute("order", order);
        return "order_success";
    }

    @GetMapping("/my-orders")
    public String viewMyOrders(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = currentUser(userDetails);
        model.addAttribute("orders", printOrderService.getOrdersByUser(user));
        return "student_orders";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        User user = currentUser(userDetails);
        try {
            printOrderService.cancelOwnOrder(user, id);
            redirectAttributes.addFlashAttribute("success", "Order cancelled.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/my-orders";
    }

    private User currentUser(CustomUserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Logged-in user not found in database"));
    }
}
