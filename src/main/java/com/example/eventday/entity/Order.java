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

    @Column(name = "order_number", nullable = false, unique = true, length = 50, columnDefinition = "VARCHAR(50)")
    private String orderNumber;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "admin_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal adminFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    public enum OrderStatus {
        PENDING, SUCCESS, EXPIRED, CANCELLED
    }
}