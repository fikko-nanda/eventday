package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.service.OrganizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizer")
@RequiredArgsConstructor
public class OrganizerController {

    private final OrganizerService organizerService;

    // ==========================================
    // SPEC V1.3.0 (MODUL 07)
    // ==========================================

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerOrganizer(@RequestBody Map<String, Object> request) {
        Map<String, Object> data = organizerService.registerOrganizer(request);
        return ResponseEntity.ok(ApiResponse.ok("Pendaftaran Event Organizer berhasil dikirim", data));
    }

    @PostMapping("/documents/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "KTP") String documentType) {

        Map<String, Object> data = organizerService.uploadDocument(file, documentType);
        return ResponseEntity.ok(ApiResponse.ok("Dokumen berhasil diunggah", data));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrganizerStatus() {
        Map<String, Object> data = organizerService.getOrganizerStatus();
        return ResponseEntity.ok(ApiResponse.ok("Status verifikasi berhasil diambil", data));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrganizerDashboard() {
        Map<String, Object> data = organizerService.getOrganizerDashboard();
        return ResponseEntity.ok(ApiResponse.ok("Data dashboard organizer berhasil diambil", data));
    }

    // ==========================================
    // PERMINTAAN UI/UX: 1. PROFIL & LEGALITAS EO
    // ==========================================

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile() {
        return ResponseEntity.ok(ApiResponse.ok("Profil organizer berhasil diambil", organizerService.getProfile()));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(ApiResponse.ok("Profil organizer berhasil diperbarui", organizerService.updateProfile(payload)));
    }

    @PostMapping("/profile/avatar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok("Foto profil organizer berhasil diunggah", organizerService.uploadAvatar(file)));
    }

    @PostMapping("/profile/upload-portfolio")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadPortfolio(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok("Portofolio EO berhasil diunggah", organizerService.uploadPortfolio(file)));
    }

    @PostMapping("/profile/upload-deed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadDeed(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok("Akta badan hukum berhasil diunggah", organizerService.uploadDeed(file)));
    }

    @GetMapping("/profile/document")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfileDocuments() {
        return ResponseEntity.ok(ApiResponse.ok("Daftar dokumen legalitas berhasil diambil", organizerService.getProfileDocuments()));
    }

    // ==========================================
    // PERMINTAAN UI/UX: 2. AUTENTIKASI EO
    // ==========================================

    @PostMapping("/auth/change-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changePassword(@RequestBody Map<String, Object> payload) {
        organizerService.changePassword(payload);
        return ResponseEntity.ok(ApiResponse.ok("Password akun organizer berhasil diubah", null));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout() {
        organizerService.logout();
        return ResponseEntity.ok(ApiResponse.ok("Organizer berhasil logout", null));
    }

    // ==========================================
    // PERMINTAAN UI/UX: 3. MANAJEMEN REFUND EO
    // ==========================================

    @GetMapping("/refunds")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRefundRequests() {
        return ResponseEntity.ok(ApiResponse.ok("Daftar permintaan refund berhasil diambil", organizerService.getRefundRequests()));
    }

    @GetMapping("/refunds/detail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRefundDetail(@RequestParam("id") String refundId) {
        return ResponseEntity.ok(ApiResponse.ok("Detail refund berhasil diambil", organizerService.getRefundDetail(refundId)));
    }

    @PatchMapping("/refunds/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRefundStatus(
            @PathVariable("id") String refundId,
            @RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(ApiResponse.ok("Status refund berhasil diperbarui", organizerService.updateRefundStatus(refundId, payload)));
    }

    // ==========================================
    // PERMINTAAN UI/UX: 4. PAYOUT & SALDO EO
    // ==========================================

    @GetMapping("/bank-accounts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBankAccounts() {
        return ResponseEntity.ok(ApiResponse.ok("Daftar rekening bank berhasil diambil", organizerService.getBankAccounts()));
    }

    @GetMapping("/events/{id}/payout-balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPayoutBalance(@PathVariable("id") Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok("Saldo payout event berhasil diambil", organizerService.getPayoutBalance(eventId)));
    }

    @GetMapping("/payouts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPayouts() {
        return ResponseEntity.ok(ApiResponse.ok("Riwayat payout berhasil diambil", organizerService.getPayouts()));
    }

    @PostMapping("/payouts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestPayout(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ApiResponse.ok("Pengajuan payout berhasil dikirim", organizerService.createPayout(request)));
    }

    @GetMapping("/payouts/detail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPayoutDetail(@RequestParam("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Detail payout berhasil diambil", organizerService.getPayoutDetail(id)));
    }

    // ==========================================
    // PERMINTAAN UI/UX: 5. MANAJEMEN EVENT EO
    // ==========================================

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOrganizerEvents() {
        return ResponseEntity.ok(ApiResponse.ok("Daftar event milik organizer berhasil diambil", Collections.emptyList()));
    }

    @PostMapping("/events")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOrganizerEvent(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("eventId", UUID.randomUUID().toString());
        response.put("status", "DRAFT");
        return ResponseEntity.ok(ApiResponse.ok("Draft event berhasil dibuat", response));
    }

    @PutMapping("/events/update")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateOrganizerEvent(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ApiResponse.ok("Detail event berhasil diperbarui", request));
    }

    @PostMapping("/events/publish")
    public ResponseEntity<ApiResponse<Map<String, Object>>> publishOrganizerEvent(@RequestParam("id") Long id) {
        Map<String, Object> response = new HashMap<>();
        response.put("eventId", id);
        response.put("status", "PUBLISHED");
        return ResponseEntity.ok(ApiResponse.ok("Event berhasil dipublikasikan", response));
    }

    @GetMapping("/events/draft")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDraftEvents() {
        return ResponseEntity.ok(ApiResponse.ok("Daftar draft event berhasil diambil", Collections.emptyList()));
    }

    @GetMapping("/events/{id}/sales-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSalesSummary(@PathVariable("id") Long id) {
        Map<String, Object> response = new HashMap<>();
        response.put("eventId", id);
        response.put("ticketsSold", 0);
        response.put("totalRevenue", 0);
        return ResponseEntity.ok(ApiResponse.ok("Ringkasan penjualan tiket event berhasil diambil", response));
    }

    // ==========================================
    // PERMINTAAN UI/UX: 6. DASHBOARD METRICS EO
    // ==========================================

    @GetMapping("/dashboard/metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardMetrics() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalEvents", 0);
        response.put("totalRevenue", 0);
        response.put("totalTicketsSold", 0);
        return ResponseEntity.ok(ApiResponse.ok("Metrik dashboard organizer berhasil diambil", response));
    }

    @GetMapping("/dashboard/recent-events")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentEvents() {
        return ResponseEntity.ok(ApiResponse.ok("Daftar event terbaru organizer berhasil diambil", Collections.emptyList()));
    }

    @GetMapping("/dashboard/recent-transactions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentTransactions() {
        return ResponseEntity.ok(ApiResponse.ok("Daftar transaksi terbaru organizer berhasil diambil", Collections.emptyList()));
    }
}