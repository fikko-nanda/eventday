package com.example.eventday.controller;

import com.example.eventday.dto.AuthResponse;
import com.example.eventday.dto.GoogleLoginRequest;
import com.example.eventday.dto.LoginRequest;
import com.example.eventday.dto.RegisterRequest;
import com.example.eventday.dto.ResetPasswordRequest;
import com.example.eventday.dto.VerifyOtpRequest;
import com.example.eventday.dto.ResendOtpRequest;
import com.example.eventday.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            AuthResponse response = authService.loginWithGoogle(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            authService.verifyOtp(request.getEmail(), request.getOtpCode());
            return ResponseEntity.ok(Map.of("message", "OTP terverifikasi! Akun aktif, silakan login."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        try {
            authService.resendOtp(request.getEmail());
            return ResponseEntity.ok(Map.of("message", "OTP baru berhasil dikirim ke email Anda!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            // MODE 1: Kirim OTP — body hanya {email}
            if (request.getCode() == null || request.getCode().isBlank()) {
                authService.sendResetCode(request.getEmail());
                return ResponseEntity.ok(Map.of("message", "Kode OTP berhasil dikirim ke email Anda!"));
            }
            // MODE 2: Verifikasi OTP saja — body {email, code} tanpa newPassword → frontend klik "Lanjutkan" → pindah halaman new password
            if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
                authService.verifyResetCode(request.getEmail(), request.getCode());
                return ResponseEntity.ok(Map.of("message", "Kode OTP valid! Silakan buat password baru."));
            }
            // MODE 3: Reset password — body {email, code, newPassword}
            if (request.getNewPassword().length() < 6) {
                return ResponseEntity.badRequest().body("Password baru minimal 6 karakter");
            }
            authService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password berhasil direset! Silakan login dengan password baru."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
