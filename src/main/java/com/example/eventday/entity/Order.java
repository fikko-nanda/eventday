package com.example.eventday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", updatable = false, nullable = false)
    private UUID orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id", nullable = false)
    private TicketTier ticketTier;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "admin_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal adminFee;

    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "payment_method", length = 50, columnDefinition = "VARCHAR(50)")
    private String paymentMethod;

    @Column(name = "transaction_id_gateway", length = 100, columnDefinition = "VARCHAR(100)")
    private String transactionIdGateway;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

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
