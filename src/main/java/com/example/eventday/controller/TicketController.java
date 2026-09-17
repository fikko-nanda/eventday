package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.dto.TicketDetailResponse;
import com.example.eventday.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    // Ambil daftar tiket murni berbasis email dari JWT Auth
    @GetMapping("/my-tickets")
    public ResponseEntity<ApiResponse<List<?>>> getMyTickets(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.badRequest("Pengguna tidak terautentikasi"));
        }
        String currentEmail = authentication.getName();
        return ResponseEntity.ok(ApiResponse.success("Daftar tiket user", ticketService.getTicketsByEmail(currentEmail)));
    }

    // Anti-IDOR: Validasi pemilik tiket atau role Panitia/Admin
    @GetMapping("/issued-detail")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> getIssuedDetail(
            @RequestParam String ticketCode,
            Authentication authentication) {
        try {
            TicketDetailResponse detail = ticketService.getIssuedDetail(ticketCode);

            // Cek apakah user yang login adalah pemilik tiket atau panitia
            if (authentication != null && authentication.getName() != null) {
                String currentUserEmail = authentication.getName();
                boolean isOwner = currentUserEmail.equalsIgnoreCase(detail.getAttendeeEmail());
                boolean isStaff = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_ORGANIZER"));

                if (!isOwner && !isStaff) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.badRequest("Anda tidak memiliki akses ke detail tiket ini"));
                }
            }

            return ResponseEntity.ok(ApiResponse.success("Detail E-Ticket berhasil dimuat", detail));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    // Hanya ROLE_ADMIN atau ROLE_ORGANIZER yang bisa me-scan tiket di venue
    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> scanTicket(@RequestBody Map<String, String> payload) {
        String ticketCode = payload.get("ticketCode");
        if (ticketCode == null || ticketCode.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("Kode tiket tidak boleh kosong"));
        }
        try {
            String result = ticketService.validateAndUseTicket(ticketCode);
            return ResponseEntity.ok(ApiResponse.success("Proses scan selesai", Map.of("status", result, "message", "Proses scan selesai")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }
}