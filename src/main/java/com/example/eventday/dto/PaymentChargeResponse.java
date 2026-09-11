package com.example.eventday.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentChargeResponse {
    private UUID orderId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String bankCode;
    private String virtualAccountNumber;
    private LocalDateTime expiredAt;
}