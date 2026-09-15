package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/organizer")
public class OrganizerController {

    // ==========================================
    // SPEC V1.3.0 (MODUL 07)
    // ==========================================

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerOrganizer(@RequestBody Map<String, Object> request) {
        Map<String, Object> data = new HashMap<>();
        data.put("organizer_name", request.getOrDefault("name", "PT Penyelenggara Event"));
        data.put("verification_status", "PENDING");
        return ResponseEntity.ok(ApiResponse.ok("Pendaftaran Event Organizer berhasil dikirim", data));
    }

    @PostMapping("/documents/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "KTP") String documentType) {

        Map<String, Object> data = new HashMap<>();
        data.put("document_type", documentType);
        data.put("file_name", file.getOriginalFilename());
        data.put("file_size", file.getSize());
        return ResponseEntity.ok(ApiResponse.ok("Dokumen berhasil diunggah", data));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrganizerStatus() {
        Map<String, Object> data = new HashMap<>();
        data.put("organizer_name", "PT Penyelenggara Event");
        data.put("verification_status", "PENDING");
        return ResponseEntity.ok(ApiResponse.ok("Status verifikasi berhasil diambil", data));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrganizerDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("total_revenue", 42500000);
        data.put("active_events", 2);
        data.put("tickets_sold", 1248);
        return ResponseEntity.ok(ApiResponse.ok("Data dashboard organizer berhasil diambil", data));
    }

    // ==========================================
    // PERMINTAAN UI/UX: 1. PROFIL & LEGALITAS EO
    // ==========================================

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", "PT Harmoni Musik Indonesia");
        profile.put("pic_name", "Budi Santoso");
        profile.put("email", "budi.santoso@harmoni.co.id");
        profile.put("phone", "+6281234567890");
        profile.put("npwp", "01.234.567.8-901.000");
        profile.put("avatar_url", "https://api.dicebear.com/7.x/identicon/svg?seed=harmoni");
        return ResponseEntity.ok(ApiResponse.ok("Profil organizer berhasil diambil", profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(ApiResponse.ok("Profil organizer berhasil diperbarui", payload));
    }

    @PostMapping("/profile/avatar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Map<String, Object> data = new HashMap<>();
        data.put("avatar_url", "https://storage.eventday.id/avatars/" + file.getOriginalFilename());
        return ResponseEntity.ok(ApiResponse.ok("Foto profil organizer berhasil diunggah", data));
    }

    @PostMapping("/profile/upload-portfolio")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadPortfolio(@RequestParam("file") MultipartFile file) {
        Map<String, Object> data = new HashMap<>();
        data.put("portfolio_url", "https://storage.eventday.id/docs/" + file.getOriginalFilename());
        return ResponseEntity.ok(ApiResponse.ok("Portofolio EO berhasil diunggah", data));
    }

    @PostMapping("/profile/upload-deed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadDeed(@RequestParam("file") MultipartFile file) {
        Map<String, Object> data = new HashMap<>();
        data.put("company_deed_url", "https://storage.eventday.id/docs/" + file.getOriginalFilename());
        return ResponseEntity.ok(ApiResponse.ok("Akta badan hukum berhasil diunggah", data));
    }

    @GetMapping("/profile/document")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfileDocuments() {
        Map<String, Object> docs = new HashMap<>();
        docs.put("portfolio_name", "CV_Portofolio.pdf");
        docs.put("deed_name", "Akta_Perusahaan.pdf");
        docs.put("ktp_name", "KTP_PIC.jpg");
        return ResponseEntity.ok(ApiResponse.ok("Daftar dokumen legalitas berhasil diambil", docs));
    }

    // ==========================================
    // PERMINTAAN UI/UX: 2. AUTENTIKASI EO
    // ==========================================

    @PostMapping("/auth/change-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changePassword(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(ApiResponse.ok("Password akun organizer berhasil diubah", null));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout() {
        return ResponseEntity.ok(ApiResponse.ok("Organizer berhasil logout", null));
    }
}