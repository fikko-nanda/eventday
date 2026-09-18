package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.service.OrganizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizer")
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
    // DASHBOARD EO METRICS & ACTIVITIES (REAL DB)
    // ==========================================

    @GetMapping("/dashboard/metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrganizerDashboardMetrics() {
        return ResponseEntity.ok(ApiResponse.ok("Metrik dashboard organizer berhasil diambil", organizerService.getOrganizerDashboardMetrics()));
    }

    @GetMapping("/dashboard/recent-events")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentEvents() {
        return ResponseEntity.ok(ApiResponse.ok("Event terbaru organizer berhasil diambil", organizerService.getRecentEvents()));
    }

    @GetMapping("/dashboard/recent-transactions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentTransactions() {
        return ResponseEntity.ok(ApiResponse.ok("Transaksi terbaru berhasil diambil", organizerService.getRecentTransactions()));
    }

    // ==========================================
    // MANAJEMEN EVENT EO (REAL PERSISTEN DB)
    // ==========================================

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOrganizerEvents() {
        return ResponseEntity.ok(ApiResponse.ok("Daftar event organizer berhasil diambil", organizerService.getOrganizerEvents()));
    }

    @PostMapping("/events")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createEvent(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body(ApiResponse.ok("Event berhasil dibuat", organizerService.createEvent(request)));
    }

    @PutMapping("/events/update")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateEvent(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ApiResponse.ok("Event berhasil diperbarui", organizerService.updateEvent(request)));
    }

    @PostMapping("/events/publish")
    public ResponseEntity<ApiResponse<Map<String, Object>>> publishEvent(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ApiResponse.ok("Event berhasil dipublikasikan", organizerService.publishEvent(request)));
    }

    @GetMapping("/events/draft")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDraftEvents() {
        return ResponseEntity.ok(ApiResponse.ok("Daftar draft event berhasil diambil", organizerService.getDraftEvents()));
    }

    @GetMapping("/events/{id}/sales-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEventSalesSummary(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Ringkasan penjualan event berhasil diambil", organizerService.getEventSalesSummary(id)));
    }

    // ==========================================
    // PROFIL & LEGALITAS EO
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
    // AUTENTIKASI EO
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
    // MANAJEMEN REFUND EO
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
    // PAYOUT & SALDO EO
    // ==========================================

    @GetMapping("/bank-accounts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBankAccounts() {
        return ResponseEntity.ok(ApiResponse.ok("Daftar rekening bank berhasil diambil", organizerService.getBankAccounts()));
    }

    @GetMapping("/events/{id}/payout-balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPayoutBalance(@PathVariable("id") UUID eventId) {
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPayoutDetail(@RequestParam("id") String id) {
        return ResponseEntity.ok(ApiResponse.ok("Detail payout berhasil diambil", organizerService.getPayoutDetailByString(id)));
    }
}