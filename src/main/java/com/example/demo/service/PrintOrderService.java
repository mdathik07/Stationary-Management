package com.example.demo.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.example.demo.controller.dto.AdminDashboardStats;
import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.PrintOrder;
import com.example.demo.entity.PrintType;
import com.example.demo.entity.User;

public interface PrintOrderService {

    PrintOrder createOrder(User user, MultipartFile file, PrintType printType,
                           boolean doubleSided, int copies, String notes);

    List<PrintOrder> getOrdersByUser(User user);

    /** Cancels the student's own order; only allowed while it is still PENDING. */
    void cancelOwnOrder(User user, Long orderId);

    /** Moves an order to the next status in the fulfilment flow. */
    void advanceStatus(Long orderId);

    /** Admin cleanup of a terminal (picked-up / cancelled) order. */
    void deleteOrder(Long orderId);

    PrintOrder getOrder(Long orderId);

    /**
     * Orders for the admin list. filter = "active" (default work queue,
     * oldest first), "all", or an {@link OrderStatus} name.
     */
    List<PrintOrder> getOrdersForAdmin(String filter, String search);

    AdminDashboardStats getDashboardStats();
}
