package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
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

    // 1. Endpoint untuk Frontend mengambil daftar tiket milik user
    @GetMapping("/user/{email}")
    public ResponseEntity<ApiResponse<List<TicketItem>>> getUserTickets(@PathVariable String email) {
        return ResponseEntity.ok(ApiResponse.success("Daftar tiket user", ticketService.getTicketsByEmail(email)));
    }

    // 2. Endpoint untuk Panitia / Admin me-scan QR Code Tiket
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