package com.example.eventday.controller;

import com.example.eventday.dto.*;
import com.example.eventday.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/methods")
    public ResponseEntity<ApiResponse<List<String>>> getPaymentMethods() {
        return ResponseEntity.ok(ApiResponse.success("Metode pembayaran", List.of("VIRTUAL_ACCOUNT", "E_WALLET", "CREDIT_CARD")));
    }

    @GetMapping("/methods/virtual-account")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getVaChannels() {
        List<Map<String, String>> channels = List.of(
                Map.of("code", "BCA", "name", "BCA Virtual Account"),
                Map.of("code", "MANDIRI", "name", "Mandiri Bill Payment"),
                Map.of("code", "BRI", "name", "BRI Virtual Account")
        );
        return ResponseEntity.ok(ApiResponse.success("Daftar channel VA", channels));
    }

    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<PaymentChargeResponse>> chargePayment(@RequestBody PaymentChargeRequest request) {
        PaymentChargeResponse chargeResponse = paymentService.processPaymentCharge(request);
        return ResponseEntity.ok(ApiResponse.success("Tagihan pembayaran berhasil dibuat", chargeResponse));
    }
}