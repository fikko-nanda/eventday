package com.example.eventday.controller.organizer;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.service.organizer.OrganizerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organizer/dashboard")
@RequiredArgsConstructor
public class OrganizerDashboardController {

    private final OrganizerDashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrganizerDashboard() {
        return ResponseEntity.ok(ApiResponse.ok("Data dashboard organizer berhasil diambil", dashboardService.getOrganizerDashboard()));
    }

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrganizerDashboardMetrics() {
        return ResponseEntity.ok(ApiResponse.ok("Metrik dashboard organizer berhasil diambil", dashboardService.getOrganizerDashboardMetrics()));
    }

    @GetMapping("/recent-events")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentEvents() {
        return ResponseEntity.ok(ApiResponse.ok("Event terbaru organizer berhasil diambil", dashboardService.getRecentEvents()));
    }

    @GetMapping("/recent-transactions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentTransactions() {
        return ResponseEntity.ok(ApiResponse.ok("Transaksi terbaru berhasil diambil", dashboardService.getRecentTransactions()));
    }
}