package com.example.eventday.controller;

import com.example.eventday.dto.RefundRequestDto;
import com.example.eventday.entity.RefundRequest;
import com.example.eventday.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    public ResponseEntity<RefundRequest> createRefund(@RequestBody RefundRequestDto dto) {
        return ResponseEntity.ok(refundService.createRefundRequest(dto));
    }

    @GetMapping
    public ResponseEntity<List<RefundRequest>> getAllRefunds() {
        return ResponseEntity.ok(refundService.getAllRefunds());
    }

    @PutMapping("/{refundId}/approve")
    public ResponseEntity<RefundRequest> approveRefund(@PathVariable UUID refundId) {
        return ResponseEntity.ok(refundService.approveRefund(refundId));
    }

    @PutMapping("/{refundId}/reject")
    public ResponseEntity<RefundRequest> rejectRefund(@PathVariable UUID refundId,
                                                      @RequestParam(required = false) String note) {
        return ResponseEntity.ok(refundService.rejectRefund(refundId, note));
    }

    @PutMapping("/{refundId}/refunded")
    public ResponseEntity<RefundRequest> markRefunded(@PathVariable UUID refundId) {
        return ResponseEntity.ok(refundService.markRefunded(refundId));
    }
}