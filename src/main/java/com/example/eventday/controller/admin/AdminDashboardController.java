package com.example.eventday.controller.admin;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.dto.TransactionHistoryResponse;
import com.example.eventday.dto.admin.AdminDashboardMetricsResponse;
import com.example.eventday.service.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/metrics")
    public ApiResponse<AdminDashboardMetricsResponse> getMetrics() {
        return ApiResponse.ok("Berhasil mengambil metrik admin", dashboardService.getDashboardMetrics());
    }

    @GetMapping("/recent-events")
    public ApiResponse<List<Map<String, Object>>> getRecentEvents() {
        return ApiResponse.ok("Berhasil mengambil event terbaru platform", dashboardService.getRecentEvents());
    }

    @GetMapping("/recent-transactions")
    public ApiResponse<List<TransactionHistoryResponse>> getRecentTransactions() {
        return ApiResponse.ok("Berhasil mengambil transaksi global terbaru", dashboardService.getRecentTransactions());
    }
}