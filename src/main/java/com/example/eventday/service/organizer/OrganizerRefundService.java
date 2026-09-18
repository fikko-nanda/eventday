package com.example.eventday.service.organizer;

import com.example.eventday.entity.Organizer;
import com.example.eventday.entity.RefundRequestEntity;
import com.example.eventday.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizerRefundService {

    private final OrganizerHelperService helperService;
    private final RefundRepository refundRepository;

    public List<Map<String, Object>> getRefundRequests() {
        Organizer org = helperService.resolveCurrentOrganizer();
        List<RefundRequestEntity> list = refundRepository.findByOrganizerId(org.getOrganizerId());

        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        return list.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("refund_id", r.getRefundId() != null ? r.getRefundId().toString() : null);
            m.put("order_id", r.getOrderId() != null ? r.getOrderId().toString() : null);
            m.put("customer_id", r.getCustomerId() != null ? r.getCustomerId().toString() : null);
            m.put("amount", r.getAmount());
            m.put("reason", r.getReason());
            m.put("bank_name", r.getBankName());
            m.put("account_number", r.getBankAccountNumber());
            m.put("account_holder", r.getAccountHolder());
            m.put("status", r.getStatus());
            m.put("created_at", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
            return m;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getRefundDetail(String refundId) {
        Organizer org = helperService.resolveCurrentOrganizer();
        UUID id;
        try {
            id = UUID.fromString(refundId);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format ID refund tidak valid");
        }

        RefundRequestEntity r = refundRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pengajuan refund tidak ditemukan"));

        if (r.getOrganizerId() == null || !r.getOrganizerId().equals(org.getOrganizerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Akses ditolak: Anda bukan pemilik tiket/event ini");
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("refund_id", r.getRefundId().toString());
        detail.put("order_id", r.getOrderId() != null ? r.getOrderId().toString() : null);
        detail.put("amount", r.getAmount());
        detail.put("reason", r.getReason());
        detail.put("bank_name", r.getBankName());
        detail.put("account_number", r.getBankAccountNumber());
        detail.put("account_holder", r.getAccountHolder());
        detail.put("status", r.getStatus());
        detail.put("rejection_reason", r.getRejectionReason());
        detail.put("admin_note", r.getAdminNote());
        detail.put("created_at", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        return detail;
    }

    @Transactional
    public Map<String, Object> updateRefundStatus(String refundId, Map<String, Object> payload) {
        Organizer org = helperService.resolveCurrentOrganizer();
        UUID id;
        try {
            id = UUID.fromString(refundId);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format ID refund tidak valid");
        }

        RefundRequestEntity r = refundRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Data refund tidak ditemukan"));

        if (r.getOrganizerId() == null || !r.getOrganizerId().equals(org.getOrganizerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Akses ditolak: Tidak memiliki hak atas event ini");
        }

        if ("APPROVED".equalsIgnoreCase(r.getStatus()) || "REJECTED".equalsIgnoreCase(r.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Pengajuan refund sudah diproses sebelumnya (" + r.getStatus() + ")"
            );
        }

        String rawStatus = payload != null ? (String) payload.getOrDefault("status", payload.get("newStatus")) : null;
        String newStatus = (rawStatus != null && !rawStatus.isBlank()) ? rawStatus.trim().toUpperCase() : "APPROVED";

        if (!"APPROVED".equals(newStatus) && !"REJECTED".equals(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status refund tidak valid (hanya APPROVED atau REJECTED)");
        }

        r.setStatus(newStatus);
        if (payload != null) {
            if (payload.containsKey("adminNote")) r.setAdminNote((String) payload.get("adminNote"));
            if (payload.containsKey("admin_note")) r.setAdminNote((String) payload.get("admin_note"));
            if (payload.containsKey("rejection_reason")) r.setRejectionReason((String) payload.get("rejection_reason"));
        }
        r.setProcessedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        refundRepository.save(r);

        Map<String, Object> res = new HashMap<>();
        res.put("refund_id", r.getRefundId().toString());
        res.put("status", r.getStatus());
        res.put("admin_note", r.getAdminNote());
        res.put("processed_at", r.getProcessedAt() != null ? r.getProcessedAt().toString() : null);
        return res;
    }
}