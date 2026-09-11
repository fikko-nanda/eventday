package com.example.eventday.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CalculationResponse {
    private BigDecimal subtotal;
    private BigDecimal adminFee;
    private BigDecimal tax;
    private BigDecimal discount;
    private BigDecimal totalAmount;
}