package com.example.eventday.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminEventResponse {

    private UUID eventId;
    private UUID organizerId;
    private String organizerName;
    private String title;
    private String description;
    private String category;
    private String venueName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private Boolean isFeatured;
    private String bannerUrl;
    private String facility;
    private String lineup;
    private List<TierInfo> ticketTiers;
    private SalesSummary salesSummary;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TierInfo {
        private UUID tierId;
        private String tierName;
        private BigDecimal price;
        private Integer totalQuota;
        private Integer availableQuota;
        private Long soldCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesSummary {
        private Long totalOrders;
        private Long ticketsSold;
        private BigDecimal revenuePaid;
        private BigDecimal revenuePending;
    }
}
