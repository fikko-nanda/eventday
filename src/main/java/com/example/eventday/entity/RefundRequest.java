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

    // Relasi ke Order (Customer memilih order mana yang mau di-refund)
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Relasi ke User (Customer yang mengajukan)
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount;

    // Info Rekening Bank Tujuan Transfer Refund
    @Column(name = "bank_name", nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    private String bankName;

    @Column(name = "bank_account_number", nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    private String bankAccountNumber;

    @Column(name = "bank_account_name", nullable = false, length = 100, columnDefinition = "VARCHAR(100)")
    private String bankAccountName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    @Builder.Default
    private RefundStatus status = RefundStatus.PENDING;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "requested_at", updatable = false)
    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public enum RefundStatus {
        PENDING, APPROVED, REJECTED, REFUNDED
    }
}