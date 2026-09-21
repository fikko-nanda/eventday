package com.example.eventday.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminEventSalesResponse {

    private Long totalOrders;
    private Long totalTicketsSold;
    private BigDecimal totalRevenuePaid;
    private BigDecimal totalRevenuePending;
    private Map<String, TierSales> salesByTier;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TierSales {
        private String tierName;
        private Integer totalQuota;
        private Integer availableQuota;
        private Long soldCount;
        private BigDecimal revenue;
    }
}
