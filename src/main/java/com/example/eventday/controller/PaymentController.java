package com.example.eventday.controller;

import com.example.eventday.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay/{orderId}")
    public ResponseEntity<?> pay(@PathVariable UUID orderId, @RequestParam String paymentMethod) {
        try {
            return ResponseEntity.ok(paymentService.payOrder(orderId, paymentMethod));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}