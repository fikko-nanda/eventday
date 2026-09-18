package com.example.eventday.controller.organizer;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.service.organizer.OrganizerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/organizer/profile")
@RequiredArgsConstructor
public class OrganizerProfileController {

    private final OrganizerProfileService profileService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile() {
        return ResponseEntity.ok(ApiResponse.ok("Profil organizer berhasil diambil", profileService.getProfile()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(ApiResponse.ok("Profil organizer berhasil diperbarui", profileService.updateProfile(payload)));
    }

    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok("Foto profil organizer berhasil diunggah", profileService.uploadAvatar(file)));
    }

    @PostMapping("/upload-portfolio")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadPortfolio(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok("Portofolio EO berhasil diunggah", profileService.uploadPortfolio(file)));
    }

    @PostMapping("/upload-deed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadDeed(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok("Akta badan hukum berhasil diunggah", profileService.uploadDeed(file)));
    }

    @GetMapping("/document")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfileDocuments() {
        return ResponseEntity.ok(ApiResponse.ok("Daftar dokumen legalitas berhasil diambil", profileService.getProfileDocuments()));
    }
}