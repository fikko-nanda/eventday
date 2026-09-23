package com.example.eventday.controller.admin;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.dto.admin.AdminTransactionListResponse;
import com.example.eventday.dto.admin.AdminTransactionResponse;
import com.example.eventday.dto.admin.AdminTransactionStatusRequest;
import com.example.eventday.service.admin.AdminTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping({"/admin/transactions", "/api/admin/transactions"})
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AdminTransactionController {

    private final AdminTransactionService adminTransactionService;

    private UUID getAdminId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    @GetMapping
    public ApiResponse<Page<AdminTransactionListResponse>> getTransactions(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "eventId", required = false) UUID eventId,
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam(value = "dateFrom", required = false) LocalDateTime dateFrom,
            @RequestParam(value = "dateTo", required = false) LocalDateTime dateTo,
            @RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
            @RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.ok("Berhasil mengambil daftar transaksi",
                adminTransactionService.getTransactions(status, eventId, userId, dateFrom, dateTo, minAmount, maxAmount, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminTransactionResponse> getTransactionDetail(@PathVariable("id") UUID orderId) {
        return ApiResponse.ok("Berhasil mengambil detail transaksi", adminTransactionService.getTransactionDetail(orderId));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<String> updateTransactionStatus(
            @PathVariable("id") UUID orderId,
            @Valid @RequestBody AdminTransactionStatusRequest request,
            Authentication authentication) {
        adminTransactionService.updateTransactionStatus(orderId, request.getStatus(), getAdminId(authentication));
        return ApiResponse.ok("Status transaksi berhasil diperbarui", null);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTransactionsCsv(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "eventId", required = false) UUID eventId,
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam(value = "dateFrom", required = false) LocalDateTime dateFrom,
            @RequestParam(value = "dateTo", required = false) LocalDateTime dateTo,
            @RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
            @RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount) {
        byte[] csv = adminTransactionService.exportTransactionsCsv(status, eventId, userId, dateFrom, dateTo, minAmount, maxAmount);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions.csv\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }
}
