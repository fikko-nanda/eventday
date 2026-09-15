package com.example.eventday.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RefundDetailResponse {
    private UUID refundId;
    private UUID orderId;
    private BigDecimal amount;
    private String reason;
    private String bankName;
    private String accountNumber;
    private String accountHolderName;
    private String status; // PENDING, APPROVED, REJECTED, COMPLETED
    private String proofUrl;
    private LocalDateTime createdAt;
}