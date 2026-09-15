package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.dto.BankResponse;
import com.example.eventday.dto.RefundDetailResponse;
import com.example.eventday.dto.RefundRequest;
import com.example.eventday.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApiResponse<List<BankResponse>>> getSupportedBanks() {
        return ResponseEntity.ok(ApiResponse.success("Daftar bank pendukung", refundService.getSupportedBanks()));
    }

    // Spec v1.3.0: /refund/order-summary
    @GetMapping("/refund/order-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderSummary(@RequestParam UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success("Ringkasan nominal refund", refundService.getRefundOrderSummary(orderId)));
    }

    // Spec v1.3.0: /refund/refund-detail/info
    @GetMapping("/refund/refund-detail/info")
    public ResponseEntity<ApiResponse<RefundDetailResponse>> getRefundDetail(@RequestParam UUID refundId) {
        return ResponseEntity.ok(ApiResponse.success("Detail status refund", refundService.getRefundDetail(refundId)));
    }

    // Spec v1.3.0: /refund/refund-detail/download-proof
    @GetMapping("/refund/refund-detail/download-proof")
    public ResponseEntity<ApiResponse<Map<String, String>>> downloadProof(@RequestParam UUID refundId) {
        RefundDetailResponse detail = refundService.getRefundDetail(refundId);
        return ResponseEntity.ok(ApiResponse.success("URL Bukti transfer refund", Map.of("proofUrl", detail.getProofUrl())));
    }

    // Spec v1.3.0: /tickets/refund/refund-history
    @GetMapping("/tickets/refund/refund-history")
    public ResponseEntity<ApiResponse<List<RefundDetailResponse>>> getRefundHistory(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success("Riwayat pengajuan refund", refundService.getRefundHistory(email)));
    }
}