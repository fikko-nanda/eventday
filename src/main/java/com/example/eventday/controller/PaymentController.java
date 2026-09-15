package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.service.MidtransService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final MidtransService midtransService;

    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<Map<String, String>>> processPayment(@RequestBody Map<String, Object> request) {
        String orderId = (String) request.get("orderId");
        BigDecimal grossAmount = new BigDecimal(request.get("grossAmount").toString());
        String customerName = (String) request.get("customerName");
        String customerEmail = (String) request.get("customerEmail");

        Map<String, String> midtransResponse = midtransService.createSnapTransaction(orderId, grossAmount, customerName, customerEmail);

        return ResponseEntity.ok(ApiResponse.success("Snap Token berhasil dibuat", midtransResponse));
    }

    @PostMapping("/midtrans-notification")
    public ResponseEntity<ApiResponse<String>> handleMidtransNotification(@RequestBody Map<String, Object> notification) {
        String orderId = (String) notification.get("order_id");
        String transactionStatus = (String) notification.get("transaction_status");

        log.info("Notifikasi Midtrans diterima untuk Order ID: {} dengan status: {}", orderId, transactionStatus);

        if ("settlement".equals(transactionStatus) || "capture".equals(transactionStatus)) {
            // Logic update order status -> PAID
        } else if ("cancel".equals(transactionStatus) || "expire".equals(transactionStatus)) {
            // Logic update order status -> EXPIRED/CANCELLED
        }

        return ResponseEntity.ok(ApiResponse.success("Notifikasi Midtrans berhasil diproses", "OK"));
    }
}