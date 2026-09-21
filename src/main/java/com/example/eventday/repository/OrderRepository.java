package com.example.eventday.repository;

import com.example.eventday.entity.Event;
import com.example.eventday.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
    List<Order> findByCustomerUserId(UUID customerId);
    List<Order> findByEvent(Event event);

    // ===== MODUL SUPERADMIN: DASHBOARD =====
    List<Order> findTop10ByOrderByCreatedAtDesc();
    List<Order> findByStatusInAndExpiredAtBefore(List<String> statuses, LocalDateTime now);
}