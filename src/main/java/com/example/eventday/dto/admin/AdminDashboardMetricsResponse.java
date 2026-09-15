package com.example.eventday.dto.admin; 

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardMetricsResponse {
    private BigDecimal totalPlatformRevenue;
    private long totalEvents;
    private long activeEvents;
    private long totalUsers;
    private long totalTicketsSold;
}