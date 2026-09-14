package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.dto.TicketDetailResponse;
import com.example.eventday.entity.TicketItem;
import com.example.eventday.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    // 1. Endpoint untuk Frontend mengambil daftar tiket milik user (Spec: /tickets/my-tickets)
    @GetMapping({"/user/{email}", "/my-tickets"})
    public ResponseEntity<ApiResponse<List<TicketItem>>> getUserTickets(
            @PathVariable(required = false) String email,
            @RequestParam(required = false) String userEmail) {
        
        String targetEmail = (email != null) ? email : userEmail;
        return ResponseEntity.ok(ApiResponse.success("Daftar tiket user", ticketService.getTicketsByEmail(targetEmail)));
    }

    // 2. Endpoint baru untuk E-Ticket Payload (Spec v1.3.0: /tickets/issued-detail)
    @GetMapping("/issued-detail")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> getIssuedDetail(@RequestParam String ticketCode) {
        try {
            TicketDetailResponse detail = ticketService.getIssuedDetail(ticketCode);
            return ResponseEntity.ok(ApiResponse.success("Detail E-Ticket berhasil dimuat", detail));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    // 3. Endpoint untuk Panitia / Admin me-scan QR Code Tiket (Spec: /tickets/scan)
    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<Map<String, String>>> scanTicket(@RequestBody Map<String, String> payload) {
        String ticketCode = payload.get("ticketCode");
        try {
            String result = ticketService.validateAndUseTicket(ticketCode);
            return ResponseEntity.ok(ApiResponse.success("Proses scan selesai", Map.of("status", result, "message", "Proses scan selesai")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }
}