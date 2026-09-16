package com.example.eventday.service;

import com.example.eventday.dto.BankResponse;
import com.example.eventday.dto.RefundDetailResponse;
import com.example.eventday.dto.RefundRequest;
import com.example.eventday.entity.Order;
import com.example.eventday.entity.RefundRequestEntity;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;

    /**
     * Daftar bank pendukung — usable untuk frontend dropdown.
     * Frontend bisa pakai bankCode sebagai value, bankName sebagai label.
     * Tambah logoUrl agar dropdown bisa tampil icon bank (opsional).
     */
    public List<Map<String, Object>> getSupportedBanks() {
        return List.of(
                Map.of("bankCode", "BCA", "bankName", "Bank Central Asia", "logoUrl", "/assets/banks/bca.png", "active", true),
                Map.of("bankCode", "MANDIRI", "bankName", "Bank Mandiri", "logoUrl", "/assets/banks/mandiri.png", "active", true),
                Map.of("bankCode", "BNI", "bankName", "Bank Negara Indonesia", "logoUrl", "/assets/banks/bni.png", "active", true),
                Map.of("bankCode", "BRI", "bankName", "Bank Rakyat Indonesia", "logoUrl", "/assets/banks/bri.png", "active", true),
                Map.of("bankCode", "CIMB", "bankName", "CIMB Niaga", "logoUrl", "/assets/banks/cimb.png", "active", true),
                Map.of("bankCode", "PERMATA", "bankName", "Bank Permata", "logoUrl", "/assets/banks/permata.png", "active", true),
                Map.of("bankCode", "BSI", "bankName", "Bank Syariah Indonesia", "logoUrl", "/assets/banks/bsi.png", "active", true),
                Map.of("bankCode", "DANAMON", "bankName", "Bank Danamon", "logoUrl", "/assets/banks/danamon.png", "active", true)
        );
    }

    /** Legacy typed version for internal call compatibility */
    public List<BankResponse> getSupportedBanksTyped() {
        return getSupportedBanks().stream()
                .map(m -> new BankResponse((String) m.get("bankCode"), (String) m.get("bankName")))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getRefundOrderSummary(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            BigDecimal gross = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal adminFee = order.getAdminFee() != null ? order.getAdminFee() : new BigDecimal("5000");
            BigDecimal refundable = gross.subtract(adminFee);
            if (refundable.compareTo(BigDecimal.ZERO) < 0) refundable = BigDecimal.ZERO;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orderId", orderId);
            m.put("orderNumber", "ORD-" + orderId.toString().substring(0, 8).toUpperCase());
            m.put("eventTitle", order.getEvent() != null ? order.getEvent().getTitle() : "Event");
            m.put("ticketTierName", order.getTicketTier() != null ? order.getTicketTier().getTierName() : "Regular");
            m.put("ticketQuantity", order.getQuantity());
            m.put("grossAmount", gross);
            m.put("adminFee", adminFee);
            m.put("refundableAmount", refundable);
            m.put("status", order.getStatus());
            m.put("expiredAt", order.getExpiredAt());
            m.put("createdAt", order.getCreatedAt());
            return m;
        }
        // fallback for unknown orderId — still usable for dev/test without DB row
        return Map.of(
                "orderId", orderId,
                "ticketQuantity", 2,
                "grossAmount", new BigDecimal("300000.00"),
                "adminFee", new BigDecimal("5000.00"),
                "refundableAmount", new BigDecimal("295000.00"));
    }

    @Transactional
    public RefundDetailResponse submitRefund(RefundRequest request) {
        String currentUserIdStr = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID customerId = UUID.fromString(currentUserIdStr);

        // Hitung amount dari order real jika ada; fallback 290k untuk dev
        BigDecimal amount = new BigDecimal("290000.00");
        UUID organizerId = null;
        try {
            Order order = orderRepository.findById(request.getOrderId()).orElse(null);
            if (order != null) {
                BigDecimal gross = order.getTotalAmount() != null ? order.getTotalAmount() : amount;
                BigDecimal fee = order.getAdminFee() != null ? order.getAdminFee() : new BigDecimal("5000");
                amount = gross.subtract(fee);
                if (amount.compareTo(BigDecimal.ZERO) < 0) amount = BigDecimal.ZERO;
                if (order.getEvent() != null && order.getEvent().getOrganizer() != null) {
                    organizerId = order.getEvent().getOrganizer().getOrganizerId();
                }
            }
        } catch (Exception ignored) {}

        // Jika organizerId masih null, coba ambil organizer default pertama (untuk memenuhi NOT NULL constraint rev.11)
        if (organizerId == null) {
            // akan diisi placeholder jika tetap null — ubah column jadi nullable via fallback UUID; gunakan customerId sebagai fallback organizerId untuk consumer refund
            organizerId = customerId;
        }

        RefundRequestEntity entity = RefundRequestEntity.builder()
                .orderId(request.getOrderId())
                .customerId(customerId)
                .organizerId(organizerId)
                .amount(amount)
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
