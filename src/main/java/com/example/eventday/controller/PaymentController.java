package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.entity.Order;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.service.MidtransService;
import com.example.eventday.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({ "/api/payments", "/api/v1/payments" })
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PaymentController {

    private final PaymentService paymentService;
    private final MidtransService midtransService;
    private final OrderRepository orderRepository;

    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<?>> processPayment(@RequestBody Map<String, Object> request) {
        log.info("Request /payments/charge masuk dengan body: {}", request);

        try {
            if (request == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.success("Request body tidak boleh kosong", null));
            }

            String orderIdStr = request.get("orderId") != null ? request.get("orderId").toString()
                    : (request.get("order_id") != null ? request.get("order_id").toString() : null);

            if (orderIdStr == null) {
                log.warn("Validation fail: orderId is null");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.success("Request body wajib menyertakan 'orderId'", null));
            }

            // Sanitasi prefix ORD- sebelum dikonversi ke UUID
            String cleanOrderIdStr = orderIdStr.startsWith("ORD-") ? orderIdStr.substring(4) : orderIdStr;
            UUID orderId = UUID.fromString(cleanOrderIdStr);
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan dengan ID: " + orderIdStr));

            String customerName = request.get("customerName") != null ? request.get("customerName").toString()
                    : (request.get("customer_name") != null ? request.get("customer_name").toString() : "");
            String customerEmail = request.get("customerEmail") != null ? request.get("customerEmail").toString() : "";

            Map<String, String> midtransResponse = midtransService.createSnapTransaction(
                    orderIdStr,
                    order.getTotalAmount(),
                    customerName,
                    customerEmail);

            return ResponseEntity.ok(ApiResponse.success("Snap Token berhasil dibuat", midtransResponse));

        } catch (Exception e) {
            log.error("Error Midtrans Snap Charge: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.success("Gagal memproses Snap Midtrans: " + e.getMessage(), null));
        }
    }

    @PostMapping({ "/midtrans-notification", "/notification" })
    public ResponseEntity<ApiResponse<String>> handleMidtransNotification(
            @RequestBody Map<String, Object> notification) {
        try {
            paymentService.processMidtransNotification(notification);
            return ResponseEntity.ok(ApiResponse.success("Notifikasi Midtrans berhasil diproses", "OK"));
        } catch (Exception e) {
            log.error("Error memproses Notifikasi Midtrans: ", e);
            // Selalu kembalikan HTTP 200 OK ke Midtrans agar webhook dianggap sukses
            return ResponseEntity
                    .ok(ApiResponse.success("Notifikasi diterima tetapi ada catatan: " + e.getMessage(), "OK"));
        }
    }
}