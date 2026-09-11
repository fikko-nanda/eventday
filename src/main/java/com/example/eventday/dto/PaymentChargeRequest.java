package com.example.eventday.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentChargeRequest {
    private UUID orderId;
    private String paymentMethod; // e.g., "VIRTUAL_ACCOUNT"
    private String bankCode;      // e.g., "BCA", "MANDIRI", "BRI"
}