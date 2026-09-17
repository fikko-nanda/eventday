package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.dto.AuthResponse;
import com.example.eventday.dto.GoogleLoginRequest;
import com.example.eventday.dto.LoginRequest;
import com.example.eventday.dto.RegisterRequest;
import com.example.eventday.dto.ResendOtpRequest;
import com.example.eventday.dto.ResetPasswordRequest;
import com.example.eventday.dto.VerifyOtpRequest;
import com.example.eventday.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SuppressWarnings("null")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse data = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(data.getMessage(), data));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse data = authService.login(request);
            ResponseCookie cookie = ResponseCookie.from("access_token", data.getToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(data.getExpiresIn())
                    .sameSite("None")
                    .build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString() + "; Partitioned")
                    .body(ApiResponse.ok(data.getMessage(), data));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            AuthResponse data = authService.loginWithGoogle(request);
            ResponseCookie cookie = ResponseCookie.from("access_token", data.getToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(data.getExpiresIn())
                    .sameSite("None")
                    .build();
            if (data.getMessage().contains("Registrasi")) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .header(HttpHeaders.SET_COOKIE, cookie.toString() + "; Partitioned")
                        .body(ApiResponse.created(data.getMessage(), data));
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString() + "; Partitioned")
                    .body(ApiResponse.ok(data.getMessage(), data));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            String msg = authService.verifyOtp(request.getEmail(), request.getOtpCode());
            return ResponseEntity.ok(ApiResponse.ok(msg));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        try {
            String msg = authService.resendOtp(request.getEmail());
            return ResponseEntity.ok(ApiResponse.ok(msg));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            String msg = authService.resetPassword(request);
            return ResponseEntity.ok(ApiResponse.ok(msg));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }
}
