package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.service.MidtransService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

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

        // Panggil Midtrans Snap Service
        Map<String, String> midtransResponse = midtransService.createSnapTransaction(orderId, grossAmount, customerName, customerEmail);

        return ResponseEntity.ok(ApiResponse.success("Snap Token berhasil dibuat", midtransResponse));
    }

    // Callback / Webhook Endpoint dari Midtrans ketika status pembayaran berubah
    @PostMapping("/midtrans-notification")
    public ResponseEntity<ApiResponse<String>> handleMidtransNotification(@RequestBody Map<String, Object> notification) {
        String orderId = (String) notification.get("order_id");
        String transactionStatus = (String) notification.get("transaction_status");

        // Logic Update Status Order di DB berdasarkan callback Midtrans
        if ("settlement".equals(transactionStatus) || "capture".equals(transactionStatus)) {
            // Update order status -> PAID, lalu panggil ticketService.generateTicket(...)
        } else if ("cancel".equals(transactionStatus) || "expire".equals(transactionStatus)) {
            // Update order status -> EXPIRED/CANCELLED
        }

        return ResponseEntity.ok(ApiResponse.success("Notifikasi Midtrans berhasil diproses", "OK"));
    }
}