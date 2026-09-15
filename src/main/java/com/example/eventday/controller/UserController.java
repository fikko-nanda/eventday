package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.dto.ChangePasswordRequest;
import com.example.eventday.dto.TransactionHistoryResponse;
import com.example.eventday.dto.UpdateProfileRequest;
import com.example.eventday.dto.UserProfileResponse;
import com.example.eventday.service.AuthService;
import com.example.eventday.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    private UUID getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Sesi login tidak ditemukan atau telah kedaluwarsa!");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Format User ID pada sesi tidak valid!");
        }
    }

    @GetMapping("/user/profile")
    public ApiResponse<UserProfileResponse> getProfile(Authentication authentication) {
        UUID userId = getAuthenticatedUserId(authentication);
        return ApiResponse.ok("Berhasil mengambil data profil", userService.getProfile(userId));
    }

    @PutMapping("/user/profile/save")
    public ApiResponse<UserProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = getAuthenticatedUserId(authentication);
        return ApiResponse.ok("Profil berhasil diperbarui", userService.updateProfile(userId, request));
    }

    @PutMapping("/account/change-password")
    public ApiResponse<String> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = getAuthenticatedUserId(authentication);
        userService.changePassword(userId, request);
        return ApiResponse.ok("Password berhasil diubah", null);
    }

    @PostMapping("/user/avatar")
    public ApiResponse<String> uploadAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        getAuthenticatedUserId(authentication);
        if (file.isEmpty()) {
            return ApiResponse.badRequest("File avatar tidak boleh kosong!");
        }
        // Placeholder return URL mock:
        String mockUrl = "/uploads/avatars/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        return ApiResponse.ok("Avatar berhasil diperbarui", mockUrl);
    }

    @PostMapping("/user/logout")
    public ApiResponse<String> logout(Authentication authentication, HttpServletResponse response) {
        if (authentication != null && authentication.getName() != null) {
            try {
                UUID userId = UUID.fromString(authentication.getName());
                authService.logout(userId);
            } catch (Exception ignored) {}
        }

        // Hapus HttpOnly cookie
        ResponseCookie cookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString() + "; Partitioned");

        return ApiResponse.ok("Logout berhasil", null);
    }

    @GetMapping("/transactions/history")
    public ApiResponse<List<TransactionHistoryResponse>> getTransactionHistory(Authentication authentication) {
        UUID userId = getAuthenticatedUserId(authentication);
        return ApiResponse.ok("Berhasil mengambil riwayat transaksi", userService.getTransactionHistory(userId));
    }
}