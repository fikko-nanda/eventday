package com.example.eventday.service.admin;

import com.example.eventday.dto.admin.PayoutDetailResponse;
import com.example.eventday.dto.admin.PayoutResponse;
import com.example.eventday.dto.admin.UpdatePayoutStatusRequest;
import com.example.eventday.entity.Organizer;
import com.example.eventday.entity.RefundRequestEntity;
import com.example.eventday.repository.OrganizerRepository;
import com.example.eventday.repository.RefundRepository;
import com.example.eventday.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AdminPayoutService {

    private final RefundRepository refundRepository;
    private final OrganizerRepository organizerRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<PayoutResponse> getAllPayouts(String statusFilter) {
        List<RefundRequestEntity> payouts;
        if (statusFilter != null && !statusFilter.isBlank()) {
            payouts = refundRepository.findPayoutsWithStatus(statusFilter.toUpperCase());
        } else {
            payouts = refundRepository.findAllPayoutsByOrderByCreatedAtDesc();
        }
        return payouts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PayoutDetailResponse getDetail(UUID payoutId) {
        RefundRequestEntity payout = refundRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Pengajuan pencairan tidak ditemukan!"));
        if (payout.getOrderId() != null) {
            throw new RuntimeException("Data adalah refund customer, bukan payout");
        }
        return mapToDetailResponse(payout);
    }

    @Transactional
    public PayoutDetailResponse updateStatus(UUID payoutId, UpdatePayoutStatusRequest request, UUID adminId) {
        RefundRequestEntity payout = refundRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Pengajuan pencairan tidak ditemukan!"));

        if (payout.getOrderId() != null) {
            throw new RuntimeException("Data adalah refund customer, bukan payout");
        }

        String newStatus = request.getStatus().toUpperCase();
        payout.setStatus(newStatus);
        payout.setAdminNote(request.getAdminNote());
        payout.setUpdatedAt(LocalDateTime.now());

        if ("APPROVED".equalsIgnoreCase(newStatus)) {
            payout.setProcessedAt(LocalDateTime.now());
        }

        refundRepository.save(payout);

        Organizer organizer = organizerRepository.findById(payout.getOrganizerId()).orElse(null);
        auditLogService.log(adminId, "SUPERADMIN", "UPDATE_PAYOUT_STATUS",
                "Pengajuan payout " + payoutId + " organizer " + (organizer != null ? organizer.getNameOrganizer() : payout.getOrganizerId())
                        + " diubah menjadi: " + newStatus
                        + (request.getAdminNote() != null ? " (Catatan: " + request.getAdminNote() + ")" : ""));

        return mapToDetailResponse(payout);
    }

    @Transactional(readOnly = true)
    public PayoutDetailResponse getReconciliationDocument(UUID payoutId) {
        RefundRequestEntity payout = refundRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Pengajuan pencairan tidak ditemukan!"));
        if (payout.getOrderId() != null) {
            throw new RuntimeException("Data adalah refund customer, bukan payout");
        }
        return mapToDetailResponse(payout);
    }

    @Transactional(readOnly = true)
    public byte[] exportPayoutsCsv(String statusFilter) {
        List<PayoutResponse> payouts = getAllPayouts(statusFilter);
        StringBuilder csv = new StringBuilder();
        csv.append("payoutId,organizerId,nameOrganizer,amount,bankName,accountNumber,accountHolder,status,rejectionReason,adminNote,createdAt,updatedAt,reconciliationDocumentUrl\n");
        for (PayoutResponse p : payouts) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    p.getPayoutId(),
                    p.getOrganizerId(),
                    p.getNameOrganizer() == null ? "" : p.getNameOrganizer().replace(",", ";"),
                    p.getAmount(),
                    p.getBankName() == null ? "" : p.getBankName().replace(",", ";"),
                    p.getAccountNumber() == null ? "" : p.getAccountNumber().replace(",", ";"),
                    p.getAccountHolder() == null ? "" : p.getAccountHolder().replace(",", ";"),
                    p.getStatus(),
                    p.getRejectionReason() == null ? "" : p.getRejectionReason().replace(",", ";"),
                    p.getAdminNote() == null ? "" : p.getAdminNote().replace(",", ";"),
                    p.getCreatedAt(),
                    p.getUpdatedAt(),
                    p.getReconciliationDocumentUrl() == null ? "" : p.getReconciliationDocumentUrl().replace(",", ";")));
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private PayoutResponse mapToResponse(RefundRequestEntity r) {
        return PayoutResponse.builder()
                .payoutId(r.getRefundId())
                .organizerId(r.getOrganizerId())
                .nameOrganizer(getOrganizerName(r.getOrganizerId()))
                .amount(r.getAmount())
                .bankName(r.getBankName())
                .accountNumber(r.getBankAccountNumber())
                .accountHolder(r.getAccountHolder())
                .status(r.getStatus())
                .rejectionReason(r.getRejectionReason())
                .adminNote(r.getAdminNote())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .reconciliationDocumentUrl(r.getReconciliationDocumentUrl())
                .build();
    }

    private PayoutDetailResponse mapToDetailResponse(RefundRequestEntity r) {
        return PayoutDetailResponse.builder()
                .payoutId(r.getRefundId())
                .organizerId(r.getOrganizerId())
                .nameOrganizer(getOrganizerName(r.getOrganizerId()))
                .userEmail(null)
                .userPhone(null)
                .amount(r.getAmount())
                .bankName(r.getBankName())
                .accountNumber(r.getBankAccountNumber())
                .accountHolder(r.getAccountHolder())
                .status(r.getStatus())
                .rejectionReason(r.getRejectionReason())
                .adminNote(r.getAdminNote())
                .reconciliationDocumentUrl(r.getReconciliationDocumentUrl())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private String getOrganizerName(UUID organizerId) {
        if (organizerId == null) return "-";
        return organizerRepository.findById(organizerId)
                .map(Organizer::getNameOrganizer)
                .orElse(organizerId.toString().substring(0, 8));
    }
}
