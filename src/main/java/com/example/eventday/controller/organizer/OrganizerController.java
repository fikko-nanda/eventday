package com.example.eventday.controller.organizer;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.service.organizer.OrganizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
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
            @RequestPart(value = "data", required = false) String dataJson,
            @RequestParam Map<String, String> allParams,
            @RequestParam(value = "cvFile", required = false) MultipartFile cvFile,
            @RequestParam(value = "cv", required = false) MultipartFile cvAlias,
            @RequestParam(value = "portfolio", required = false) MultipartFile portfolioFile,
            @RequestParam(value = "portfolioFile", required = false) MultipartFile portfolioAlias,
            @RequestParam(value = "aktaFile", required = false) MultipartFile aktaFile,
            @RequestParam(value = "akta", required = false) MultipartFile aktaAlias,
            @RequestParam(value = "companyDeed", required = false) MultipartFile companyDeedFile,
            @RequestParam(value = "aktaPerusahaan", required = false) MultipartFile aktaPerusahaanFile) {

        // 1. Parse part "data" sebagai JSON String (toleran content-type octet-stream/json)
        Map<String, Object> request = new HashMap<>();
        if (dataJson != null && !dataJson.isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(dataJson, Map.class);
                request.putAll(parsed);
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("Field 'data' bukan JSON valid"));
            }
        }

        // 2. Masukkan semua field teks multipart (key apa pun) tanpa menimpa data JSON
        for (Map.Entry<String, String> e : allParams.entrySet()) {
            request.putIfAbsent(e.getKey(), e.getValue());
        }

        // 3. Alias normalization -> key kanonik (camelCase & snake_case frontend)
        fillAlias(request, "name", "organizer_name", "nama", "namaEo", "nama_eo");
        fillAlias(request, "npwp_number", "npwp", "npwpNumber");
        fillAlias(request, "bank_name", "bankName");
        fillAlias(request, "bank_account_number", "account_number", "bankAccountNumber");

        // 4. File: ambil non-kosong pertama di antara alias tiap slot (semua required=false)
        MultipartFile cv = firstFile(cvFile, cvAlias);
        MultipartFile portfolio = firstFile(portfolioFile, portfolioAlias);
        MultipartFile akta = firstFile(aktaFile, aktaAlias, companyDeedFile, aktaPerusahaanFile);

        Map<String, Object> data = organizerService.registerOrganizer(request, cv, portfolio, null, akta);
        return ResponseEntity.ok(ApiResponse.ok("Pendaftaran Event Organizer berhasil dikirim", data));
    }

    private static void fillAlias(Map<String, Object> target, String canonical, String... aliases) {
        Object existing = target.get(canonical);
        if (existing != null && !String.valueOf(existing).isBlank()) {
            return;
        }
        for (String key : aliases) {
            String v = target.get(key) != null ? String.valueOf(target.get(key)) : null;
            if (v != null && !v.isBlank()) {
                target.put(canonical, v);
                return;
            }
        }
    }

    private static MultipartFile firstFile(MultipartFile... files) {
        for (MultipartFile f : files) {
            if (f != null && !f.isEmpty()) {
                return f;
            }
        }
        return null;
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