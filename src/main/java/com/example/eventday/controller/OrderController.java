package com.example.eventday.controller;

import com.example.eventday.dto.CreateOrderRequest;
import com.example.eventday.dto.OrderResponse;
import com.example.eventday.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        try {
            OrderResponse response = orderService.createOrder(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
