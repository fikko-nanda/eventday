package com.example.eventday.service;

import com.example.eventday.dto.BankResponse;
import com.example.eventday.dto.RefundDetailResponse;
import com.example.eventday.dto.RefundRequest;
import com.example.eventday.entity.RefundRequestEntity;
import com.example.eventday.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;

    public List<BankResponse> getSupportedBanks() {
        return List.of(
                new BankResponse("BCA", "Bank Central Asia"),
                new BankResponse("MANDIRI", "Bank Mandiri"),
                new BankResponse("BNI", "Bank Negara Indonesia"),
                new BankResponse("BRI", "Bank Rakyat Indonesia"));
    }

    public Map<String, Object> getRefundOrderSummary(UUID orderId) {
        return Map.of(
                "orderId", orderId,
                "ticketQuantity", 2,
                "grossAmount", new BigDecimal("300000.00"),
                "adminFee", new BigDecimal("10000.00"),
                "refundableAmount", new BigDecimal("290000.00"));
    }

    @Transactional
    public RefundDetailResponse submitRefund(RefundRequest request) {
        // Ambil ID customer otomatis dari token JWT user yang sedang login
        String currentUserIdStr = SecurityContextHolder.getContext().getAuthentication().getName();

        RefundRequestEntity entity = RefundRequestEntity.builder()
                .orderId(request.getOrderId())
                .customerId(UUID.fromString(currentUserIdStr))
                .refundAmount(new BigDecimal("290000.00"))
                .reason(request.getReason())
                .bankName(request.getBankCode())
                .bankAccountNumber(request.getAccountNumber())
                .bankAccountName(request.getAccountHolderName())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        RefundRequestEntity saved = refundRepository.save(entity);

        return RefundDetailResponse.builder()
                .refundId(saved.getRefundId())
                .orderId(saved.getOrderId())
                .amount(saved.getRefundAmount())
                .reason(saved.getReason())
                .bankName(saved.getBankName())
                .accountNumber(saved.getBankAccountNumber())
                .accountHolderName(saved.getBankAccountName())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public RefundDetailResponse getRefundDetail(UUID refundId) {
        return refundRepository.findById(refundId)
                .map(r -> RefundDetailResponse.builder()
                        .refundId(r.getRefundId())
                        .orderId(r.getOrderId())
                        .amount(r.getRefundAmount())
                        .reason(r.getReason())
                        .bankName(r.getBankName())
                        .accountNumber(r.getBankAccountNumber())
                        .accountHolderName(r.getBankAccountName())
                        .status(r.getStatus())
                        .createdAt(r.getCreatedAt())
                        .build())
                .orElseThrow(() -> new RuntimeException("Refund tidak ditemukan!"));
    }

    public List<RefundDetailResponse> getRefundHistory(String email) {
        return List.of(
                RefundDetailResponse.builder()
                        .refundId(UUID.randomUUID())
                        .orderId(UUID.randomUUID())
                        .amount(new BigDecimal("290000.00"))
                        .reason("Acara Diundur")
                        .bankName("BCA")
                        .accountNumber("1234567890")
                        .accountHolderName("Nama Customer")
                        .status("COMPLETED")
                        .createdAt(LocalDateTime.now())
                        .build());
    }
}