package com.example.eventday.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrganizerService {

    // ==========================================
    // SPEC V1.3.0 (MODUL 07)
    // ==========================================

    public Map<String, Object> registerOrganizer(Map<String, Object> request) {
        Map<String, Object> data = new HashMap<>();
        data.put("organizer_name", request.getOrDefault("name", "PT Penyelenggara Event"));
        data.put("verification_status", "PENDING");
        return data;
    }

    public Map<String, Object> uploadDocument(MultipartFile file, String documentType) {
        Map<String, Object> data = new HashMap<>();
        data.put("document_type", documentType);
        data.put("file_name", file.getOriginalFilename());
        data.put("file_size", file.getSize());
        return data;
    }

    public Map<String, Object> getOrganizerStatus() {
        Map<String, Object> data = new HashMap<>();
        data.put("organizer_name", "PT Penyelenggara Event");
        data.put("verification_status", "PENDING");
        return data;
    }

    public Map<String, Object> getOrganizerDashboard() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("total_revenue", 42500000);
        metrics.put("active_events", 2);
        metrics.put("tickets_sold", 1248);
        return metrics;
    }

    // ==========================================
    // PROFIL & LEGALITAS EO
    // ==========================================

    public Map<String, Object> getProfile() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", "PT Harmoni Musik Indonesia");
        profile.put("pic_name", "Budi Santoso");
        profile.put("email", "budi.santoso@harmoni.co.id");
        profile.put("phone", "+6281234567890");
        profile.put("npwp", "01.234.567.8-901.000");
        profile.put("avatar_url", "https://api.dicebear.com/7.x/identicon/svg?seed=harmoni");
        return profile;
    }

    public Map<String, Object> updateProfile(Map<String, Object> payload) {
        return payload;
    }

    public Map<String, Object> uploadAvatar(MultipartFile file) {
        Map<String, Object> data = new HashMap<>();
        data.put("avatar_url", "https://storage.eventday.id/avatars/" + file.getOriginalFilename());
        return data;
    }

    public Map<String, Object> uploadPortfolio(MultipartFile file) {
        Map<String, Object> data = new HashMap<>();
        data.put("portfolio_url", "https://storage.eventday.id/docs/" + file.getOriginalFilename());
        return data;
    }

    public Map<String, Object> uploadDeed(MultipartFile file) {
        Map<String, Object> data = new HashMap<>();
        data.put("company_deed_url", "https://storage.eventday.id/docs/" + file.getOriginalFilename());
        return data;
    }

    public Map<String, Object> getProfileDocuments() {
        Map<String, Object> docs = new HashMap<>();
        docs.put("portfolio_name", "CV_Portofolio.pdf");
        docs.put("deed_name", "Akta_Perusahaan.pdf");
        docs.put("ktp_name", "KTP_PIC.jpg");
        return docs;
    }

    // ==========================================
    // AUTENTIKASI EO
    // ==========================================

    public void changePassword(Map<String, Object> payload) {
        // Business logic ubah password
    }

    public void logout() {
        // Business logic invalidasi sesi
    }

    // ==========================================
    // MANAJEMEN REFUND EO
    // ==========================================

    public List<Map<String, Object>> getRefundRequests() {
        Map<String, Object> item = new HashMap<>();
        item.put("refund_id", "REF-001");
        item.put("order_id", "ORD-12345");
        item.put("customer_name", "Farid Ainur");
        item.put("event_title", "Konser Musik Indie Fest");
        item.put("amount", 250000);
        item.put("reason", "Event dijadwalkan ulang");
        item.put("status", "PENDING");
        return List.of(item);
    }

    public Map<String, Object> getRefundDetail(String refundId) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("refund_id", refundId);
        detail.put("order_id", "ORD-12345");
        detail.put("customer_name", "Farid Ainur");
        detail.put("account_number", "9876543210");
        detail.put("bank_name", "BCA");
        detail.put("amount", 250000);
        detail.put("reason", "Event dijadwalkan ulang");
        detail.put("status", "PENDING");
        detail.put("submitted_at", "2026-09-14 10:00:00");
        return detail;
    }

    public Map<String, Object> updateRefundStatus(String refundId, Map<String, Object> payload) {
        Map<String, Object> res = new HashMap<>();
        res.put("refund_id", refundId);
        res.put("status", payload.getOrDefault("status", "APPROVED"));
        res.put("notes", payload.getOrDefault("notes", "Disetujui oleh EO"));
        return res;
    }

    // ==========================================
    // PAYOUT & SALDO EO
    // ==========================================

    public List<Map<String, Object>> getBankAccounts() {
        Map<String, Object> bank = new HashMap<>();
        bank.put("id", 1);
        bank.put("bank_name", "BCA");
        bank.put("account_number", "8830123456");
        bank.put("account_holder", "PT Harmoni Musik Indonesia");
        return List.of(bank);
    }

    public Map<String, Object> getPayoutBalance(Long eventId) {
        Map<String, Object> balance = new HashMap<>();
        balance.put("event_id", eventId);
        balance.put("total_sales", 50000000);
        balance.put("withdrawable_balance", 45000000);
        balance.put("pending_payout", 5000000);
        return balance;
    }

    public List<Map<String, Object>> getPayouts() {
        Map<String, Object> payout = new HashMap<>();
        payout.put("id", 101);
        payout.put("amount", 25000000);
        payout.put("status", "SUCCESS");
        payout.put("requested_at", "2026-09-10");
        return List.of(payout);
    }

    public Map<String, Object> createPayout(Map<String, Object> request) {
        Map<String, Object> payout = new HashMap<>();
        payout.put("payout_id", 102);
        payout.put("amount", request.getOrDefault("amount", 10000000));
        payout.put("status", "PENDING_APPROVAL");
        return payout;
    }

    public Map<String, Object> getPayoutDetail(Long id) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", id);
        detail.put("amount", 25000000);
        detail.put("status", "SUCCESS");
        detail.put("bank_name", "BCA");
        detail.put("account_number", "8830123456");
        detail.put("transfer_proof_url", "https://storage.eventday.id/proofs/transfer-101.png");
        return detail;
    }
}