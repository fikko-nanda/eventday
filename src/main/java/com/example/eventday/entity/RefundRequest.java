package com.example.eventday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refund_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "refund_id", updatable = false, nullable = false)
    private UUID refundId;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = true)
    private Order order;

    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "bank_name", nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    private String bankName;

    @Column(name = "bank_account_number", nullable = false, length = 35, columnDefinition = "VARCHAR(35)")
    private String bankAccountNumber;

    @Column(name = "bank_account_name", nullable = false, length = 100, columnDefinition = "VARCHAR(100)")
    private String bankAccountName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "requested_at", updatable = false)
    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "create_by")
    private UUID createBy;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "updated_by")
    private UUID updatedBy;
}