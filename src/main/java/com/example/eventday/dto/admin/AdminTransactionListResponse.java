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
public class AdminTransactionListResponse {

    private UUID orderId;
    private String orderNumber;
    private String eventTitle;
    private String tierName;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String status;
    private String customerName;
    private String customerEmail;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
