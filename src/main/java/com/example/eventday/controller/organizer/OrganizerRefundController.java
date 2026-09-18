package com.example.eventday.controller.organizer;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.service.organizer.OrganizerRefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organizer/refunds")
@RequiredArgsConstructor
public class OrganizerRefundController {

    private final OrganizerRefundService refundService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRefundRequests() {
        return ResponseEntity.ok(ApiResponse.ok("Daftar permintaan refund berhasil diambil", refundService.getRefundRequests()));
    }

    @GetMapping("/detail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRefundDetail(@RequestParam("id") String refundId) {
        return ResponseEntity.ok(ApiResponse.ok("Detail refund berhasil diambil", refundService.getRefundDetail(refundId)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRefundStatus(
            @PathVariable("id") String refundId,
            @RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(ApiResponse.ok("Status refund berhasil diperbarui", refundService.updateRefundStatus(refundId, payload)));
    }
}