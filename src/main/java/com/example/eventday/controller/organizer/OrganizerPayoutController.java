package com.example.eventday.controller.organizer;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.service.organizer.OrganizerPayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizer")
@RequiredArgsConstructor
public class OrganizerPayoutController {

    private final OrganizerPayoutService payoutService;

    @GetMapping("/bank-accounts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBankAccounts() {
        return ResponseEntity.ok(ApiResponse.ok("Daftar rekening bank berhasil diambil", payoutService.getBankAccounts()));
    }

    @GetMapping("/events/{id}/payout-balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPayoutBalance(@PathVariable("id") UUID eventId) {
        return ResponseEntity.ok(ApiResponse.ok("Saldo payout event berhasil diambil", payoutService.getPayoutBalance(eventId)));
    }

    @GetMapping("/payouts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPayouts() {
        return ResponseEntity.ok(ApiResponse.ok("Riwayat payout berhasil diambil", payoutService.getPayouts()));
    }

    @PostMapping("/payouts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestPayout(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ApiResponse.ok("Pengajuan payout berhasil dikirim", payoutService.createPayout(request)));
    }

    @GetMapping("/payouts/detail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPayoutDetail(@RequestParam("id") String id) {
        return ResponseEntity.ok(ApiResponse.ok("Detail payout berhasil diambil", payoutService.getPayoutDetailByString(id)));
    }
}