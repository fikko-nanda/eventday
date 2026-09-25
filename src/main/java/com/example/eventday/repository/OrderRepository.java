package com.example.eventday.repository;

import com.example.eventday.entity.Event;
import com.example.eventday.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
    List<Order> findByCustomerUserId(UUID customerId);
    List<Order> findByEvent(Event event);

    // Cek order aktif (PENDING / WAITING_PAYMENT) milik user untuk tier tiket tertentu yang belum expired
    // Menggunakan 'findFirst' & 'OrderByCreatedAtDesc' agar hanya mengambil 1 order terbaru jika terdapat lebih dari 1 order
    Optional<Order> findFirstByCustomerUserIdAndTicketTierTierIdAndStatusInAndExpiredAtAfterOrderByCreatedAtDesc(
            UUID customerId,
            UUID tierId,
            List<String> statuses,
            LocalDateTime now
    );

    // ===== MODUL SUPERADMIN: DASHBOARD =====
    List<Order> findTop10ByOrderByCreatedAtDesc();
    List<Order> findByStatusInAndExpiredAtBefore(List<String> statuses, LocalDateTime now);

    // ===== ORGANIZER FINANCE: Gross Revenue (PAID only, milik organizer) =====
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'PAID' AND o.event.organizer.organizerId = :organizerId")
    BigDecimal sumPaidRevenueByOrganizer(@Param("organizerId") UUID organizerId);
}