package com.example.eventday.repository;

import com.example.eventday.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByCustomerUserId(UUID customerId);

    // ===== MODUL SUPERADMIN: DASHBOARD =====
    List<Order> findTop10ByOrderByCreatedAtDesc();
}