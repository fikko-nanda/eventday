package com.example.eventday.controller.admin;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.dto.admin.AdminUserListItemResponse;
import com.example.eventday.dto.admin.AdminUserStatusRequest;
import com.example.eventday.service.admin.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/admin/users", "/api/admin/users"})
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    private UUID getAdminId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Sesi admin tidak valid!");
        }
        return UUID.fromString(authentication.getName());
    }

    @GetMapping
    public ApiResponse<List<AdminUserListItemResponse>> getUsers(
            @RequestParam(value = "role", required = false) String role) {
        return ApiResponse.ok("Berhasil mengambil daftar pengguna", adminUserService.getAllUsers(role));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminUserListItemResponse> getUserDetail(@PathVariable("id") UUID userId) {
        return ApiResponse.ok("Berhasil mengambil detail pengguna", adminUserService.getUserDetail(userId));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<String> updateStatus(
            @PathVariable("id") UUID userId,
            @Valid @RequestBody AdminUserStatusRequest request,
            Authentication authentication) {
        UUID adminId = getAdminId(authentication);
        adminUserService.updateUserStatus(userId, request.getStatus(), adminId);
        return ApiResponse.ok("Status pengguna berhasil diperbarui", null);
    }

    @PatchMapping("/{id}/suspend")
    public ApiResponse<String> suspendUser(
            @PathVariable("id") UUID userId,
            Authentication authentication) {
        UUID adminId = getAdminId(authentication);
        adminUserService.suspendUser(userId, adminId);
        return ApiResponse.ok("Akun pengguna berhasil ditangguhkan (SUSPENDED)", null);
    }
}