package com.example.eventday.dto.admin;

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
public class AdminTransactionResponse {

    private UUID orderId;
    private String orderNumber;
    private UUID eventId;
    private String eventTitle;
    private String eventVenue;
    private String tierName;
    private BigDecimal tierPrice;
    private Integer quantity;
    private BigDecimal subtotal;
    private BigDecimal adminFee;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;
    private String transactionIdGateway;
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private LocalDateTime paidAt;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
}
