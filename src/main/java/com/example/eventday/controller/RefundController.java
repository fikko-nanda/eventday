package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.dto.RefundDetailResponse;
import com.example.eventday.dto.RefundRequest;
import com.example.eventday.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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

    // Spec v1.3.0: /tickets/refund/request & /refund/submit
    @PostMapping({"/tickets/refund/request", "/refund/submit"})
    public ResponseEntity<ApiResponse<RefundDetailResponse>> submitRefund(@RequestBody RefundRequest request) {
        RefundDetailResponse response = refundService.submitRefund(request);
        return ResponseEntity.ok(ApiResponse.success("Pengajuan refund berhasil dibuat", response));
    }

    // Spec v1.3.0: /refund/banks
    @GetMapping("/refund/banks")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSupportedBanks() {
        return ResponseEntity.ok(ApiResponse.success("Daftar bank pendukung", refundService.getSupportedBanks()));
    }

    // Spec v1.3.0: /refund/order-summary
    @GetMapping("/refund/order-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderSummary(@RequestParam("orderId") UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success("Ringkasan nominal refund", refundService.getRefundOrderSummary(orderId)));
    }

    // Spec v1.3.0: /refund/refund-detail/info
    @GetMapping("/refund/refund-detail/info")
    public ResponseEntity<ApiResponse<RefundDetailResponse>> getRefundDetail(@RequestParam("refundId") UUID refundId) {
        return ResponseEntity.ok(ApiResponse.success("Detail status refund", refundService.getRefundDetail(refundId)));
    }

    // Spec v1.3.0: /refund/refund-detail/download-proof
    @GetMapping("/refund/refund-detail/download-proof")
    public ResponseEntity<ApiResponse<Map<String, String>>> downloadProof(@RequestParam("refundId") UUID refundId) {
        RefundDetailResponse detail = refundService.getRefundDetail(refundId);
        String proofUrl = detail.getProofUrl() != null ? detail.getProofUrl() : "";
        return ResponseEntity.ok(ApiResponse.success("URL Bukti transfer refund", Collections.singletonMap("proofUrl", proofUrl)));
    }

    // Spec v1.3.0: /tickets/refund/refund-history
    @GetMapping("/tickets/refund/refund-history")
    public ResponseEntity<ApiResponse<List<RefundDetailResponse>>> getRefundHistory() {
        String currentUserIdStr = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID customerId = UUID.fromString(currentUserIdStr);

        List<RefundDetailResponse> history = refundService.getRefundHistoryByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success("Riwayat pengajuan refund", history));
    }
}