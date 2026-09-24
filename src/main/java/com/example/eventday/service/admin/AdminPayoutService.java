package com.example.eventday.service.admin;

import com.example.eventday.dto.admin.PayoutDetailResponse;
import com.example.eventday.dto.admin.PayoutResponse;
import com.example.eventday.dto.admin.UpdatePayoutStatusRequest;
import com.example.eventday.entity.Organizer;
import com.example.eventday.entity.OrganizerPayout;
import com.example.eventday.repository.OrganizerPayoutRepository;
import com.example.eventday.repository.OrganizerRepository;
import com.example.eventday.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AdminPayoutService {

    private final OrganizerPayoutRepository organizerPayoutRepository;
    private final OrganizerRepository organizerRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<PayoutResponse> getAllPayouts(String statusFilter) {
        List<OrganizerPayout> payouts;
        if (statusFilter != null && !statusFilter.isBlank()) {
            payouts = organizerPayoutRepository.findByStatusIgnoreCase(statusFilter);
        } else {
            payouts = organizerPayoutRepository.findAllByOrderByCreatedAtDesc();
        }
        return payouts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PayoutDetailResponse getDetail(UUID payoutId) {
        OrganizerPayout payout = organizerPayoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Pengajuan pencairan tidak ditemukan!"));
        return mapToDetailResponse(payout);
    }

    @Transactional
    public PayoutDetailResponse updateStatus(UUID payoutId, UpdatePayoutStatusRequest request, UUID adminId) {
        OrganizerPayout payout = organizerPayoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Pengajuan pencairan tidak ditemukan!"));

        String newStatus = request.getStatus().toUpperCase();
        payout.setStatus(newStatus);

        organizerPayoutRepository.save(payout);

        Organizer organizer = payout.getOrganizer();
        String organizerName = organizer != null ? organizer.getNameOrganizer() : getOrganizerName(payout.getOrganizer() != null ? payout.getOrganizer().getOrganizerId() : null);

        auditLogService.log(adminId, "SUPERADMIN", "UPDATE_PAYOUT_STATUS",
                "Pengajuan payout " + payoutId + " organizer " + organizerName
                        + " diubah menjadi: " + newStatus
                        + (request.getAdminNote() != null ? " (Catatan: " + request.getAdminNote() + ")" : ""));

        return mapToDetailResponse(payout);
    }

    @Transactional(readOnly = true)
    public PayoutDetailResponse getReconciliationDocument(UUID payoutId) {
        OrganizerPayout payout = organizerPayoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Pengajuan pencairan tidak ditemukan!"));
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

    private PayoutResponse mapToResponse(OrganizerPayout p) {
        UUID organizerId = p.getOrganizer() != null ? p.getOrganizer().getOrganizerId() : null;
        String organizerName = p.getOrganizer() != null ? p.getOrganizer().getNameOrganizer() : getOrganizerName(organizerId);

        return PayoutResponse.builder()
                .payoutId(p.getPayoutId())
                .organizerId(organizerId)
                .nameOrganizer(organizerName)
                .amount(p.getAmount())
                .bankName(p.getBankName())
                .accountNumber(p.getBankAccountNumber())
                .accountHolder(p.getAccountHolder())
                .status(p.getStatus())
                .rejectionReason(null)
                .adminNote(null)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .reconciliationDocumentUrl(null)
                .build();
    }

    private PayoutDetailResponse mapToDetailResponse(OrganizerPayout p) {
        UUID organizerId = p.getOrganizer() != null ? p.getOrganizer().getOrganizerId() : null;
        String organizerName = p.getOrganizer() != null ? p.getOrganizer().getNameOrganizer() : getOrganizerName(organizerId);

        return PayoutDetailResponse.builder()
                .payoutId(p.getPayoutId())
                .organizerId(organizerId)
                .nameOrganizer(organizerName)
                .userEmail(null)
                .userPhone(null)
                .amount(p.getAmount())
                .bankName(p.getBankName())
                .accountNumber(p.getBankAccountNumber())
                .accountHolder(p.getAccountHolder())
                .status(p.getStatus())
                .rejectionReason(null)
                .adminNote(null)
                .reconciliationDocumentUrl(null)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private String getOrganizerName(UUID organizerId) {
        if (organizerId == null) return "-";
        return organizerRepository.findById(organizerId)
                .map(Organizer::getNameOrganizer)
                .orElse(organizerId.toString().substring(0, 8));
    }
}