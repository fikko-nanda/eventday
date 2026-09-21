package com.example.eventday.controller.admin;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.dto.admin.AdminEoApplicationResponse;
import com.example.eventday.dto.admin.AdminEoStatusRequest;
import com.example.eventday.service.admin.AdminEoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/admin/eo-applications", "/api/admin/eo-applications"})
@RequiredArgsConstructor
public class AdminEoController {

    private final AdminEoService adminEoService;

    private static final Logger log = LoggerFactory.getLogger(AdminEoController.class);

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    private UUID getAdminId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    @GetMapping
    public ApiResponse<List<AdminEoApplicationResponse>> getApplications(
            @RequestParam(value = "status", required = false) String status) {
        return ApiResponse.ok("Berhasil mengambil daftar aplikasi EO", adminEoService.getApplications(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminEoApplicationResponse> getApplicationDetail(@PathVariable("id") UUID organizerId) {
        return ApiResponse.ok("Berhasil mengambil detail aplikasi EO", adminEoService.getApplicationDetail(organizerId));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<String> updateStatus(
            @PathVariable("id") UUID organizerId,
            @Valid @RequestBody AdminEoStatusRequest request,
            Authentication authentication) {
        adminEoService.updateApplicationStatus(organizerId, request.getStatus(), request.getRejectionReason(), getAdminId(authentication));
        return ApiResponse.ok("Status verifikasi EO berhasil diperbarui", null);
    }

    @GetMapping("/{id}/documents/company-deed")
    public ApiResponse<Map<String, String>> getCompanyDeed(@PathVariable("id") UUID organizerId) {
        String docUrl = adminEoService.getCompanyDeedDocument(organizerId);
        return ApiResponse.ok("Berhasil mengambil tautan dokumen akta", Map.of("documentUrl", docUrl));
    }

    @GetMapping("/{id}/documents/company-deed/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadCompanyDeed(
            @PathVariable("id") UUID organizerId,
            Authentication authentication) {

        UUID adminId = UUID.fromString(authentication.getName());
        String filePath = adminEoService.getCompanyDeedDocument(organizerId);

        String fileName = filePath != null && !filePath.isEmpty()
                ? filePath.substring(filePath.lastIndexOf('/') + 1)
                : "company-deed.pdf";
        Path path = Paths.get(uploadDir, "organizer-docs", fileName);
        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(path.toFile());

        if (!resource.exists()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build();
        }

        String contentType = null;
        try {
            contentType = Files.probeContentType(path);
        } catch (IOException e) {
            log.warn("Gagal probe content type for file: {}: {}", path, e);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }
}