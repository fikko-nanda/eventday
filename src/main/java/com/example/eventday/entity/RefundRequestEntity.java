package com.example.eventday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refund_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "refund_id")
    private UUID refundId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "reason")
    private String reason;

    @Column(name = "bank_name", length = 50)
    private String bankName;

    @Column(name = "bank_account_number", length = 35)
    private String bankAccountNumber;

    @Column(name = "bank_account_name", length = 100)
    private String accountHolder;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "reconciliation_document_url", columnDefinition = "TEXT")
    private String reconciliationDocumentUrl;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
