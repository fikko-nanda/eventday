package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.dto.RefundDetailResponse;
import com.example.eventday.dto.RefundRequest;
import com.example.eventday.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping({"/tickets/refund/request", "/refund/submit"})
    public ResponseEntity<ApiResponse<RefundDetailResponse>> submitRefund(@RequestBody RefundRequest request) {
        RefundDetailResponse response = refundService.submitRefund(request);
        return ResponseEntity.ok(ApiResponse.success("Pengajuan refund berhasil dibuat", response));
    }

    @GetMapping("/refund/banks")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSupportedBanks() {
        return ResponseEntity.ok(ApiResponse.success("Daftar bank pendukung", refundService.getSupportedBanks()));
    }

    @GetMapping("/refund/order-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderSummary(@RequestParam("orderId") UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success("Ringkasan nominal refund", refundService.getRefundOrderSummary(orderId)));
    }

    // [PERBAIKAN 5]: Validasi kepemilikan detail refund
    @GetMapping("/refund/refund-detail/info")
    public ResponseEntity<ApiResponse<RefundDetailResponse>> getRefundDetail(@RequestParam("refundId") UUID refundId) {
        RefundDetailResponse detail = refundService.getRefundDetail(refundId);
        return ResponseEntity.ok(ApiResponse.success("Detail status refund", detail));
    }

    // [PERBAIKAN 5]: Validasi kepemilikan unduh bukti transfer (Anti-IDOR)
    @GetMapping("/refund/refund-detail/download-proof")
    public ResponseEntity<ApiResponse<Map<String, String>>> downloadProof(@RequestParam("refundId") UUID refundId) {
        RefundDetailResponse detail = refundService.getRefundDetail(refundId);
        String proofUrl = detail.getProofUrl() != null ? detail.getProofUrl() : "";
        return ResponseEntity.ok(ApiResponse.success("URL Bukti transfer refund", Collections.singletonMap("proofUrl", proofUrl)));
    }

    @GetMapping("/tickets/refund/refund-history")
    public ResponseEntity<ApiResponse<List<RefundDetailResponse>>> getRefundHistory(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("User belum terautentikasi"));
        }
        
        String currentUserIdStr = authentication.getName();
        UUID customerId = UUID.fromString(currentUserIdStr);

        List<RefundDetailResponse> history = refundService.getRefundHistoryByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success("Riwayat pengajuan refund", history));
    }
}