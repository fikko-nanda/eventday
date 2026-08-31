package com.example.eventday.dto;

import com.example.eventday.entity.Payment.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID paymentId;
    private UUID orderId;
    private String orderNumber;
    private String paymentMethod;
    private PaymentStatus paymentStatus;
    private String transactionIdGateway;
    private LocalDateTime paidAt;
}
