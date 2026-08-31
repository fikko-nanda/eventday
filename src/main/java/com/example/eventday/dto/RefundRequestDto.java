package com.example.eventday.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class RefundRequestDto {
    private UUID orderId;
    private UUID customerId;
    private String reason;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountName;
}