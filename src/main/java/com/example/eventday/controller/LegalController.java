package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.service.LegalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class LegalController {

    private final LegalService legalService;

    @GetMapping({ "/terms-conditions", "/api/terms-conditions" })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTermsConditions() {
        Map<String, Object> data = legalService.getTermsAndConditions();
        return ResponseEntity.ok(ApiResponse.ok("Terms and conditions retrieved successfully", data));
    }

    @GetMapping({ "/privacy-policy", "/api/privacy-policy" })

    public ResponseEntity<ApiResponse<Map<String, Object>>> getPrivacyPolicy() {
        Map<String, Object> data = legalService.getPrivacyPolicy();
        return ResponseEntity.ok(ApiResponse.ok("Privacy policy retrieved successfully", data));
    }
}