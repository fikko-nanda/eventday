package com.example.eventday.controller.admin;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.dto.admin.AdminSettingsRequest;
import com.example.eventday.entity.AuditLog;
import com.example.eventday.service.admin.AdminSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final AdminSettingsService adminSettingsService;

    private UUID getAdminId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    @GetMapping("/audit-logs")
    public ApiResponse<Page<AuditLog>> getAuditLogs(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.ok("Berhasil mengambil audit logs", adminSettingsService.getAuditLogs(page, size));
    }

    @GetMapping("/settings/general")
    public ApiResponse<Map<String, String>> getGeneralSettings() {
        return ApiResponse.ok("Berhasil mengambil pengaturan sistem", adminSettingsService.getGeneralSettings());
    }

    @PutMapping("/settings/general")
    public ApiResponse<String> updateGeneralSettings(
            @RequestBody AdminSettingsRequest request,
            Authentication authentication) {
        adminSettingsService.saveGeneralSettings(request, getAdminId(authentication));
        return ApiResponse.ok("Pengaturan sistem berhasil diperbarui", null);
    }
}