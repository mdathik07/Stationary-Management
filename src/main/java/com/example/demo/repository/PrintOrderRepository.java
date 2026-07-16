package com.example.demo.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.PrintOrder;
import com.example.demo.entity.User;

public interface PrintOrderRepository extends JpaRepository<PrintOrder, Long> {

    List<PrintOrder> findByUserOrderByCreatedAtDesc(User user);

    Optional<PrintOrder> findByOrderIdIgnoreCase(String orderId);

    /** Active work queue: oldest first so the shop prints in arrival order. */
    List<PrintOrder> findByStatusInOrderByCreatedAtAsc(Collection<OrderStatus> statuses);

    List<PrintOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<PrintOrder> findAllByOrderByCreatedAtDesc();

    long countByStatus(OrderStatus status);

    long countByCreatedAtAfter(LocalDateTime since);

    @Query("""
            select coalesce(sum(o.totalPrice), 0) from PrintOrder o
            where o.createdAt >= :since and o.status <> :excluded
            """)
    BigDecimal revenueSince(@Param("since") LocalDateTime since, @Param("excluded") OrderStatus excluded);
}
