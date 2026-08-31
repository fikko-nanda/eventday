package com.example.eventday.dto;

import com.example.eventday.entity.Order.OrderStatus;
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
public class OrderResponse {
    private UUID orderId;
    private String orderNumber;
    private String customerName;
    private String customerEmail;
    private UUID eventId;
    private String eventTitle;
    private BigDecimal totalAmount;
    private BigDecimal adminFee;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;
}
