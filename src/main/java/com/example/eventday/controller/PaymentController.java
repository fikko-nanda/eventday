package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.service.MidtransService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/api/payments", "/api/v1/payments"})
@RequiredArgsConstructor
public class PaymentController {

    private final MidtransService midtransService;

    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<?>> processPayment(@RequestBody Map<String, Object> request) {
        log.info("Request /payments/charge masuk dengan body: {}", request);

        try {
            if (request == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.success("Request body tidak boleh kosong", null));
            }

            String orderId = request.get("orderId") != null ? request.get("orderId").toString()
                    : (request.get("order_id") != null ? request.get("order_id").toString() : null);

            Object amountObj = request.get("grossAmount") != null ? request.get("grossAmount")
                    : (request.get("gross_amount") != null ? request.get("gross_amount") : request.get("amount"));

            if (orderId == null || amountObj == null) {
                log.warn("Validation fail: orderId={}, amountObj={}", orderId, amountObj);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.success("Request body wajib menyertakan 'orderId' dan 'grossAmount'", null));
            }

            BigDecimal grossAmount = new BigDecimal(amountObj.toString());
            String customerName = request.get("customerName") != null ? request.get("customerName").toString()
                    : (request.get("customer_name") != null ? request.get("customer_name").toString() : "");
            String customerEmail = request.get("customerEmail") != null ? request.get("customerEmail").toString() : "";

            Map<String, String> midtransResponse = midtransService.createSnapTransaction(orderId, grossAmount, customerName, customerEmail);

            return ResponseEntity.ok(ApiResponse.success("Snap Token berhasil dibuat", midtransResponse));

        } catch (Exception e) {
            log.error("Error Midtrans Snap Charge: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.success("Gagal memproses Snap Midtrans: " + e.getMessage(), null));
        }
    }

    @PostMapping("/midtrans-notification")
    public ResponseEntity<ApiResponse<String>> handleMidtransNotification(@RequestBody Map<String, Object> notification) {
        String orderId = (String) notification.get("order_id");
        String transactionStatus = (String) notification.get("transaction_status");

        log.info("Notifikasi Midtrans diterima untuk Order ID: {} dengan status: {}", orderId, transactionStatus);

        return ResponseEntity.ok(ApiResponse.success("Notifikasi Midtrans berhasil diproses", "OK"));
    }
}