package com.example.eventday.controller.organizer;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.service.organizer.OrganizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/organizer")
@RequiredArgsConstructor
public class OrganizerController {

    private final OrganizerService organizerService;

    // ==========================================
    // ONBOARDING & STATUS VERIFIKASI EO
    // ==========================================

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerOrganizer(
            @RequestPart("data") Map<String, Object> request,
            @RequestParam(value = "cvFile", required = false) MultipartFile cvFile,
            @RequestParam(value = "portfolio", required = false) MultipartFile portfolioFile,
            @RequestParam(value = "aktaFile", required = false) MultipartFile aktaFile,
            @RequestParam(value = "aktaPerusahaan", required = false) MultipartFile aktaPerusahaanFile) {
        Map<String, Object> data = organizerService.registerOrganizer(request, cvFile, portfolioFile, aktaFile, aktaPerusahaanFile);
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
}