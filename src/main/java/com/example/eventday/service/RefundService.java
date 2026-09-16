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
import java.util.stream.Collectors;

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
        String currentUserIdStr = SecurityContextHolder.getContext().getAuthentication().getName();

        RefundRequestEntity entity = RefundRequestEntity.builder()
                .orderId(request.getOrderId())
                .customerId(UUID.fromString(currentUserIdStr))
                .amount(new BigDecimal("290000.00"))
                .reason(request.getReason())
                .bankName(request.getBankCode())
                .bankAccountNumber(request.getAccountNumber())
                .accountHolder(request.getAccountHolderName())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        RefundRequestEntity saved = refundRepository.save(entity);

        return RefundDetailResponse.builder()
                .refundId(saved.getRefundId())
                .orderId(saved.getOrderId())
                .amount(saved.getAmount())
                .reason(saved.getReason())
                .bankName(saved.getBankName())
                .accountNumber(saved.getBankAccountNumber())
                .accountHolderName(saved.getAccountHolder())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public RefundDetailResponse getRefundDetail(UUID refundId) {
        return refundRepository.findById(refundId)
                .map(r -> RefundDetailResponse.builder()
                        .refundId(r.getRefundId())
                        .orderId(r.getOrderId())
                        .amount(r.getAmount())
                        .reason(r.getReason())
                        .bankName(r.getBankName())
                        .accountNumber(r.getBankAccountNumber())
                        .accountHolderName(r.getAccountHolder())
                        .status(r.getStatus())
                        .createdAt(r.getCreatedAt())
                        .build())
                .orElseThrow(() -> new RuntimeException("Refund tidak ditemukan!"));
    }

    // Mengambil riwayat berdasarkan customerId (Database)
    public List<RefundDetailResponse> getRefundHistoryByCustomer(UUID customerId) {
        return refundRepository.findByCustomerId(customerId).stream()
                .map(r -> RefundDetailResponse.builder()
                        .refundId(r.getRefundId())
                        .orderId(r.getOrderId())
                        .amount(r.getAmount())
                        .reason(r.getReason())
                        .bankName(r.getBankName())
                        .accountNumber(r.getBankAccountNumber())
                        .accountHolderName(r.getAccountHolder())
                        .status(r.getStatus())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // Kompatibilitas method lama (Email)
    public List<RefundDetailResponse> getRefundHistory(String email) {
        String currentUserIdStr = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            return getRefundHistoryByCustomer(UUID.fromString(currentUserIdStr));
        } catch (Exception e) {
            return List.of();
        }
    }
}
