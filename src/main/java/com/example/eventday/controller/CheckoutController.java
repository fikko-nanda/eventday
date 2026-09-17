package com.example.eventday.controller;

import com.example.eventday.dto.*;
import com.example.eventday.entity.Order;
import com.example.eventday.model.Attendee;
import com.example.eventday.service.OrderService;
import com.example.eventday.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
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

    // 1. Simpan Peserta ke Database
    @PostMapping("/checkout/attendees")
    public ResponseEntity<ApiResponse<List<Attendee>>> saveAttendees(@RequestBody AttendeeRequest request) {
        List<Attendee> result = orderService.saveAttendees(request);
        return ResponseEntity.ok(ApiResponse.success("Data peserta berhasil disimpan", result));
    }

    // 2. Hitung Rincian Biaya Checkout
    @PostMapping("/checkout/calculation")
    public ResponseEntity<ApiResponse<CalculationResponse>> calculateOrder(@RequestBody CalculationRequest request) {
        CalculationResponse calculation = orderService.calculateCheckout(request);
        return ResponseEntity.ok(ApiResponse.success("Kalkulasi checkout berhasil", calculation));
    }

    // 3. Memproses Status Checkout
    @PostMapping("/checkout/process")
    public ResponseEntity<ApiResponse<Order>> processCheckout(@RequestBody ProcessCheckoutRequest request) {
        Order order = orderService.processCheckout(request.getOrderId());
        return ResponseEntity.ok(ApiResponse.success("Checkout berhasil diproses", order));
    }

    // 4. Polling Status Order
    @GetMapping("/orders/status")
    public ResponseEntity<ApiResponse<Map<String, String>>> getOrderStatus(@RequestParam UUID orderId) {
        String status = orderService.getOrderStatus(orderId);
        return ResponseEntity.ok(ApiResponse.success("Status order ditemukan", Map.of("orderId", orderId.toString(), "status", status)));
    }

    @GetMapping("/checkout/summary")
    public ResponseEntity<ApiResponse<?>> getSummary(@RequestParam(value = "orderId", required = false) UUID orderId) {
        try {
            if (orderId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.success("Parameter orderId wajib diisi", null));
            }

            CheckoutSummaryResponse summary = paymentService.getCheckoutSummary(orderId);
            return ResponseEntity.ok(ApiResponse.success("Ringkasan checkout", summary));
        } catch (Exception e) {
            log.error("Error saat memuat ringkasan checkout orderId {}: ", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.success("Gagal memuat ringkasan checkout: " + e.getMessage(), null));
        }
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