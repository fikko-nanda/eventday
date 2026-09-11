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
public class CheckoutSummaryResponse {
    private UUID orderId;
    private String orderNumber;
    private String eventTitle;
    private String ticketTierName;
    private Integer quantity;
    private BigDecimal pricePerTicket;
    private BigDecimal subtotal;
    private BigDecimal adminFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private LocalDateTime expiredAt;
}