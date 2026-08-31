package com.example.eventday.controller;

import com.example.eventday.dto.RefundRequestDto;
import com.example.eventday.entity.RefundRequest;
import com.example.eventday.service.RefundService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping
    public ResponseEntity<RefundRequest> createRefund(@RequestBody RefundRequestDto dto) {
        return ResponseEntity.ok(refundService.createRefundRequest(dto));
    }

    @GetMapping
    public ResponseEntity<List<RefundRequest>> getAllRefunds() {
        return ResponseEntity.ok(refundService.getAllRefunds());
    }
}