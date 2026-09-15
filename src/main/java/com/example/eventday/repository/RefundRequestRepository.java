package com.example.eventday.repository;

import com.example.eventday.entity.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, UUID> {

    @Query("SELECT r FROM RefundRequest r WHERE r.refundId = :refundId AND r.customer.nik = :customerId")
    Optional<RefundRequest> findByRefundIdAndCustomerId(
            @Param("refundId") UUID refundId, 
            @Param("customerId") UUID customerId
    );

    @Query("SELECT r FROM RefundRequest r WHERE r.customer.nik = :customerId ORDER BY r.createdAt DESC")
    List<RefundRequest> findByCustomerIdOrderByCreatedAtDesc(
            @Param("customerId") UUID customerId
    );
}