package com.example.eventday.controller;

import com.example.eventday.dto.*;
import com.example.eventday.entity.Order;
import com.example.eventday.service.OrderService;
import com.example.eventday.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CheckoutController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @PostMapping("/checkout/initiate")
    public ResponseEntity<ApiResponse<Order>> initiateCheckout(
            Authentication authentication,
            @RequestBody InitiateCheckoutRequest request) {
        UUID userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof UUID) {
            userId = (UUID) authentication.getPrincipal();
        }
        Order order = orderService.createOrder(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Checkout diinisiasi", order));
    }

    @PostMapping("/checkout/attendees")
    public ResponseEntity<ApiResponse<String>> saveAttendees(@RequestBody AttendeesRequest request) {
        // Logika menyimpan identitas pemegang tiket
        return ResponseEntity.ok(ApiResponse.success("Data peserta berhasil disimpan", "OK"));
    }

    @GetMapping("/checkout/summary")
    public ResponseEntity<ApiResponse<CheckoutSummaryResponse>> getSummary(@RequestParam UUID orderId) {
        CheckoutSummaryResponse summary = paymentService.getCheckoutSummary(orderId);
        return ResponseEntity.ok(ApiResponse.success("Ringkasan checkout", summary));
    }

    @GetMapping("/orders/{orderId}/total-amount")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getTotalAmount(@PathVariable UUID orderId) {
        CheckoutSummaryResponse summary = paymentService.getCheckoutSummary(orderId);
        return ResponseEntity.ok(ApiResponse.success("Total tagihan", Map.of("totalAmount", summary.getTotalAmount())));
    }

    @GetMapping("/orders/{orderId}/expired-time")
    public ResponseEntity<ApiResponse<Map<String, LocalDateTime>>> getExpiredTime(@PathVariable UUID orderId) {
        CheckoutSummaryResponse summary = paymentService.getCheckoutSummary(orderId);
        return ResponseEntity.ok(ApiResponse.success("Waktu kadaluarsa", Map.of("expiredAt", summary.getExpiredAt())));
    }
}