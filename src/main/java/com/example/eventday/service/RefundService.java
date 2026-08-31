package com.example.eventday.service;

import com.example.eventday.dto.RefundRequestDto;
import com.example.eventday.entity.Order;
import com.example.eventday.entity.RefundRequest;
import com.example.eventday.entity.User;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.repository.RefundRequestRepository;
import com.example.eventday.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRequestRepository refundRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public RefundRequest createRefundRequest(RefundRequestDto dto) {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order tidak ditemukan!"));

        if (order.getStatus() != Order.OrderStatus.SUCCESS) {
            throw new RuntimeException("Hanya order berstatus SUCCESS yang bisa di-refund!");
        }

        User customer = userRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer tidak ditemukan!"));

        RefundRequest refund = RefundRequest.builder()
                .order(order)
                .customer(customer)
                .reason(dto.getReason())
                .refundAmount(order.getTotalAmount())
                .bankName(dto.getBankName())
                .bankAccountNumber(dto.getBankAccountNumber())
                .bankAccountName(dto.getBankAccountName())
                .status(RefundRequest.RefundStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        RefundRequest saved = refundRepository.save(refund);

        auditLogService.log(customer.getUserId(), customer.getName(), "CREATE", "RefundRequest",
                saved.getRefundId().toString(), "Pengajuan refund order " + order.getOrderNumber());

        return saved;
    }

    @Transactional
    public RefundRequest approveRefund(UUID refundId) {
        RefundRequest refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new RuntimeException("Pengajuan refund tidak ditemukan!"));

        refund.setStatus(RefundRequest.RefundStatus.APPROVED);
        refund.setProcessedAt(LocalDateTime.now());

        RefundRequest saved = refundRepository.save(refund);
        auditLogService.log(null, "Super Admin", "REVIEW", "RefundRequest",
                saved.getRefundId().toString(), "Refund disetujui untuk order " + saved.getOrder().getOrderNumber());
        return saved;
    }

    @Transactional
    public RefundRequest rejectRefund(UUID refundId, String adminNote) {
        RefundRequest refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new RuntimeException("Pengajuan refund tidak ditemukan!"));

        refund.setStatus(RefundRequest.RefundStatus.REJECTED);
        refund.setAdminNote(adminNote);
        refund.setProcessedAt(LocalDateTime.now());

        RefundRequest saved = refundRepository.save(refund);
        auditLogService.log(null, "Super Admin", "REVIEW", "RefundRequest",
                saved.getRefundId().toString(), "Refund ditolak untuk order " + saved.getOrder().getOrderNumber());
        return saved;
    }

    @Transactional
    public RefundRequest markRefunded(UUID refundId) {
        RefundRequest refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new RuntimeException("Pengajuan refund tidak ditemukan!"));

        refund.setStatus(RefundRequest.RefundStatus.REFUNDED);
        refund.setProcessedAt(LocalDateTime.now());

        RefundRequest saved = refundRepository.save(refund);
        auditLogService.log(null, "Super Admin", "TRANSFER", "RefundRequest",
                saved.getRefundId().toString(), "Dana refund ditransfer untuk order " + saved.getOrder().getOrderNumber());
        return saved;
    }

    public List<RefundRequest> getAllRefunds() {
        return refundRepository.findAll();
    }
}