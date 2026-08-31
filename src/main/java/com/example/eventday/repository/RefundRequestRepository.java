package com.example.eventday.repository;

import com.example.eventday.entity.RefundRequest;
import com.example.eventday.entity.RefundRequest.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, UUID> {
    List<RefundRequest> findByCustomerUserId(UUID customerId);
    List<RefundRequest> findByOrderOrderId(UUID orderId);
    List<RefundRequest> findByStatus(RefundStatus status);
}