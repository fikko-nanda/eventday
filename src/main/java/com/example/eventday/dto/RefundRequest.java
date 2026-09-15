package com.example.eventday.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class RefundRequest {
    private UUID orderId;
    private String reason;
    private String bankCode;
    private String accountNumber;
    private String accountHolderName;
}