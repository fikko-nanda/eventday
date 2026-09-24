package com.example.eventday.service;

import com.example.eventday.dto.BankResponse;
import com.example.eventday.dto.RefundDetailResponse;
import com.example.eventday.dto.RefundRequest;
import com.example.eventday.entity.Order;
import com.example.eventday.entity.RefundRequestEntity;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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
@SuppressWarnings("null")
public class RefundService {

    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;

    @org.springframework.beans.factory.annotation.Value("${app.refund.fee-per-ticket:2500}")
    private BigDecimal refundFeePerTicket;

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

    public List<BankResponse> getSupportedBanksTyped() {
        return getSupportedBanks().stream()
                .map(m -> new BankResponse((String) m.get("bankCode"), (String) m.get("bankName")))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getRefundOrderSummary(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order dengan ID " + orderId + " tidak ditemukan."));

        BigDecimal gross = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal adminFee = order.getAdminFee() != null ? order.getAdminFee() : new BigDecimal("5000");
        BigDecimal feeTotal = refundFeePerTicket.multiply(BigDecimal.valueOf(
                order.getQuantity() != null ? order.getQuantity() : 1));
        BigDecimal refundable = gross.subtract(adminFee).subtract(feeTotal);
        if (refundable.compareTo(BigDecimal.ZERO) < 0) refundable = BigDecimal.ZERO;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderId", orderId);
        m.put("orderNumber", "ORD-" + orderId.toString().substring(0, 8).toUpperCase());
        m.put("eventTitle", order.getEvent() != null ? order.getEvent().getTitle() : "Event");
        m.put("ticketTierName", order.getTicketTier() != null ? order.getTicketTier().getTierName() : "Regular");
        m.put("ticketQuantity", order.getQuantity());
        m.put("grossAmount", gross);
        m.put("adminFee", adminFee);
        m.put("refundFeePerTicket", refundFeePerTicket);
        m.put("refundFeeTotal", feeTotal);
        m.put("refundableAmount", refundable);
        m.put("status", order.getStatus());
        m.put("expiredAt", order.getExpiredAt());
        m.put("createdAt", order.getCreatedAt());
        return m;
    }

    @Transactional
    public RefundDetailResponse submitRefund(RefundRequest request) {
        String currentUserIdStr = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID customerId = UUID.fromString(currentUserIdStr);

        // 1. Integrasi Validasi Order
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan."));

        // 2. Pengecekan IDOR pada saat Pengajuan (Hanya pemilik order yang bisa submit)
        if (order.getCustomer() != null && !order.getCustomer().getUserId().equals(customerId)) {
            throw new AccessDeniedException("Anda tidak berhak mengajukan refund untuk transaksi ini.");
        }

        // 3. Kalkulasi Dinamis Amount — potong admin fee + biaya proses refund per tiket (ditanggung customer)
        BigDecimal gross = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal adminFee = order.getAdminFee() != null ? order.getAdminFee() : BigDecimal.ZERO;
        BigDecimal feeTotal = refundFeePerTicket.multiply(BigDecimal.valueOf(
                order.getQuantity() != null ? order.getQuantity() : 1));
        BigDecimal amount = gross.subtract(adminFee).subtract(feeTotal);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            amount = BigDecimal.ZERO;
        }

        // 4. Validasi Strict Organizer ID (Menghapus Fallback organizerId = customerId)
        if (order.getEvent() == null || order.getEvent().getOrganizer() == null || order.getEvent().getOrganizer().getOrganizerId() == null) {
            throw new IllegalStateException("Integritas data gagal: Penyelenggara (organizer) tidak valid.");
        }
        UUID organizerId = order.getEvent().getOrganizer().getOrganizerId();

        // 5. Tolak double refund pada order yang sama (hanya 1 refund aktif per order)
        List<RefundRequestEntity> existing = refundRepository.findByCustomerId(customerId);
        boolean alreadyRefunded = existing.stream().anyMatch(x ->
                request.getOrderId().equals(x.getOrderId())
                        && !"REJECTED".equalsIgnoreCase(x.getStatus()));
        if (alreadyRefunded) {
            throw new IllegalStateException("Order ini sudah memiliki pengajuan refund yang sedang diproses atau disetujui.");
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

        return mapToRefundDetailResponse(saved);
    }

    public RefundDetailResponse getRefundDetail(UUID refundId) {
        RefundRequestEntity refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Data refund tidak ditemukan."));

        // Menutup IDOR Exposure (Temuan 6)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserIdStr = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!refund.getCustomerId().toString().equalsIgnoreCase(currentUserIdStr) && !isAdmin) {
            throw new AccessDeniedException("Anda tidak memiliki hak akses untuk melihat data refund ini.");
        }

        return mapToRefundDetailResponse(refund);
    }

    public List<RefundDetailResponse> getRefundHistoryByCustomer(UUID customerId) {
        return refundRepository.findByCustomerId(customerId).stream()
                .map(this::mapToRefundDetailResponse)
                .collect(Collectors.toList());
    }

    public List<RefundDetailResponse> getRefundHistory(String email) {
        String currentUserIdStr = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            return getRefundHistoryByCustomer(UUID.fromString(currentUserIdStr));
        } catch (Exception e) {
            return List.of();
        }
    }

    private RefundDetailResponse mapToRefundDetailResponse(RefundRequestEntity entity) {
        return RefundDetailResponse.builder()
                .refundId(entity.getRefundId())
                .orderId(entity.getOrderId())
                .amount(entity.getAmount())
                .reason(entity.getReason())
                .bankName(entity.getBankName())
                .accountNumber(entity.getBankAccountNumber())
                .accountHolderName(entity.getAccountHolder())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}