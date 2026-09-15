package com.example.eventday.controller.admin;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.dto.admin.AdminEoApplicationResponse;
import com.example.eventday.dto.admin.AdminEoStatusRequest;
import com.example.eventday.service.admin.AdminEoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/eo-applications")
@RequiredArgsConstructor
public class AdminEoController {

    private final AdminEoService adminEoService;

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
}