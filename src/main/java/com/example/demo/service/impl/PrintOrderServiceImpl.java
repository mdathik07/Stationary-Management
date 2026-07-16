package com.example.demo.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.config.PricingProperties;
import com.example.demo.controller.dto.AdminDashboardStats;
import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.PrintOrder;
import com.example.demo.entity.PrintType;
import com.example.demo.entity.StoredFile;
import com.example.demo.entity.User;
import com.example.demo.repository.PrintOrderRepository;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.PrintOrderService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrintOrderServiceImpl implements PrintOrderService {

    private static final int MAX_COPIES = 100;

    private final PrintOrderRepository printOrderRepository;
    private final FileStorageService fileStorageService;
    private final PricingProperties pricing;

    @Override
    @Transactional
    public PrintOrder createOrder(User user, MultipartFile file, PrintType printType,
                                  boolean doubleSided, int copies, String notes) {
        if (copies < 1 || copies > MAX_COPIES) {
            throw new IllegalArgumentException("Copies must be between 1 and " + MAX_COPIES + ".");
        }
        if (printType == null) {
            throw new IllegalArgumentException("Please choose a print type.");
        }

        StoredFile stored = fileStorageService.storePdf(file);
        int pageCount = countPages(stored.getData());

        BigDecimal perPage = printType == PrintType.COLOR ? pricing.getColorPerPage() : pricing.getBwPerPage();
        BigDecimal totalPrice = perPage
                .multiply(BigDecimal.valueOf(pageCount))
                .multiply(BigDecimal.valueOf(copies));

        PrintOrder order = PrintOrder.builder()
                .orderId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .fileName(stored.getFileName())
                .file(stored)
                .pageCount(pageCount)
                .printType(printType)
                .doubleSided(doubleSided)
                .numberOfCopies(copies)
                .notes(notes == null ? null : notes.trim())
                .totalPrice(totalPrice)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        return printOrderRepository.save(order);
    }

    private int countPages(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            throw new IllegalArgumentException("The uploaded file is not a readable PDF.");
        }
    }

    @Override
    public List<PrintOrder> getOrdersByUser(User user) {
        return printOrderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override
    @Transactional
    public void cancelOwnOrder(User user, Long orderId) {
        PrintOrder order = getOrder(orderId);
        if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only cancel your own orders.");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only pending orders can be cancelled.");
        }
        order.setStatus(OrderStatus.CANCELLED);
        printOrderRepository.save(order);
    }

    @Override
    @Transactional
    public void advanceStatus(Long orderId) {
        PrintOrder order = getOrder(orderId);
        OrderStatus next = order.getStatus().next();
        if (next == null) {
            throw new IllegalArgumentException("Order " + order.getOrderId() + " is already closed.");
        }
        order.setStatus(next);
        if (next == OrderStatus.READY) {
            order.setReadyAt(LocalDateTime.now());
        } else if (next == OrderStatus.PICKED_UP) {
            order.setPickedUpAt(LocalDateTime.now());
        }
        printOrderRepository.save(order);
    }

    @Override
    @Transactional
    public void deleteOrder(Long orderId) {
        PrintOrder order = getOrder(orderId);
        if (!order.getStatus().isTerminal()) {
            throw new IllegalArgumentException("Only picked-up or cancelled orders can be deleted.");
        }
        printOrderRepository.delete(order);
    }

    @Override
    public PrintOrder getOrder(Long orderId) {
        return printOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found."));
    }

    @Override
    public List<PrintOrder> getOrdersForAdmin(String filter, String search) {
        if (search != null && !search.isBlank()) {
            return printOrderRepository.findByOrderIdIgnoreCase(search.trim())
                    .map(List::of)
                    .orElse(List.of());
        }
        if (filter == null || filter.isBlank() || "active".equalsIgnoreCase(filter)) {
            return printOrderRepository.findByStatusInOrderByCreatedAtAsc(
                    EnumSet.of(OrderStatus.PENDING, OrderStatus.PRINTING, OrderStatus.READY));
        }
        if ("all".equalsIgnoreCase(filter)) {
            return printOrderRepository.findAllByOrderByCreatedAtDesc();
        }
        try {
            OrderStatus status = OrderStatus.valueOf(filter.toUpperCase());
            return printOrderRepository.findByStatusOrderByCreatedAtDesc(status);
        } catch (IllegalArgumentException e) {
            return printOrderRepository.findAllByOrderByCreatedAtDesc();
        }
    }

    @Override
    public AdminDashboardStats getDashboardStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return new AdminDashboardStats(
                printOrderRepository.countByStatus(OrderStatus.PENDING),
                printOrderRepository.countByStatus(OrderStatus.PRINTING),
                printOrderRepository.countByStatus(OrderStatus.READY),
                printOrderRepository.countByCreatedAtAfter(startOfDay),
                printOrderRepository.revenueSince(startOfDay, OrderStatus.CANCELLED));
    }
}
