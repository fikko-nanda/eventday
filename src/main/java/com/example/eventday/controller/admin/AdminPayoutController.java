package com.example.eventday.controller.admin;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.dto.admin.PayoutDetailResponse;
import com.example.eventday.dto.admin.PayoutResponse;
import com.example.eventday.dto.admin.UpdatePayoutStatusRequest;
import com.example.eventday.service.admin.AdminPayoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/admin/payouts", "/api/admin/payouts"})
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AdminPayoutController {

    private final AdminPayoutService adminPayoutService;

    private UUID getAdminId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    @GetMapping
    public ApiResponse<List<PayoutResponse>> getAllPayouts(
            @RequestParam(value = "status", required = false) String statusFilter) {
        return ApiResponse.ok("Berhasil mengambil daftar pengajuan pencairan", adminPayoutService.getAllPayouts(statusFilter));
    }

    @GetMapping("/{id}")
    public ApiResponse<PayoutDetailResponse> getDetail(@PathVariable("id") UUID payoutId) {
        return ApiResponse.ok("Berhasil mengambil detail pengajuan pencairan", adminPayoutService.getDetail(payoutId));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<PayoutDetailResponse> updateStatus(
            @PathVariable("id") UUID payoutId,
            @Valid @RequestBody UpdatePayoutStatusRequest request,
            Authentication authentication) {
        return ApiResponse.ok("Status pengajuan pencairan berhasil diperbarui",
                adminPayoutService.updateStatus(payoutId, request, getAdminId(authentication)));
    }

    @GetMapping("/{id}/documents/reconciliation")
    public ApiResponse<PayoutDetailResponse> getReconciliationDocument(@PathVariable("id") UUID payoutId) {
        return ApiResponse.ok("Berhasil mengambil dokumen rekonsiliasi",
                adminPayoutService.getReconciliationDocument(payoutId));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPayoutsCsv(
            @RequestParam(value = "status", required = false) String statusFilter) {
        byte[] csv = adminPayoutService.exportPayoutsCsv(statusFilter);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payouts.csv\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }
}
