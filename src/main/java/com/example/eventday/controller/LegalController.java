package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class LegalController {

    @GetMapping("/terms-conditions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTermsConditions() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Syarat dan Ketentuan EventDay");
        data.put("content", "Selamat datang di EventDay. Dengan mengakses platform ini, Anda menyetujui seluruh syarat dan ketentuan yang berlaku.");
        data.put("updated_at", "2026-09-14");
        return ResponseEntity.ok(ApiResponse.ok("Terms and conditions retrieved successfully", data));
    }

    @GetMapping("/privacy-policy")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPrivacyPolicy() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Kebijakan Privasi EventDay");
        data.put("content", "EventDay berkomitmen penuh melindungi kerahasiaan data dan privasi setiap pengguna.");
        data.put("updated_at", "2026-09-14");
        return ResponseEntity.ok(ApiResponse.ok("Privacy policy retrieved successfully", data));
    }
}