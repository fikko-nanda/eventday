package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.service.OrganizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/organizer")
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
}