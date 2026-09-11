package com.example.eventday.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CalculationRequest {
    private UUID tierId;
    private Integer quantity;
    private BigDecimal discountAmount;
}