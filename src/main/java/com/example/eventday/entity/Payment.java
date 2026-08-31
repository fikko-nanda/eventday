package com.example.eventday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id", updatable = false, nullable = false)
    private UUID paymentId;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "payment_method", nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, columnDefinition = "VARCHAR(255)")
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "transaction_id_gateway", length = 100, columnDefinition = "VARCHAR(100)")
    private String transactionIdGateway;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED
    }
}