package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.dto.TicketDetailResponse;
import com.example.eventday.entity.TicketItem;
import com.example.eventday.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({ "/api/tickets", "/api/v1/tickets" })
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    // Menerima parameter userEmail opsional dari query request, fallback ke token
    // Authentication
    @GetMapping("/my-tickets")
    public ResponseEntity<ApiResponse<List<TicketItem>>> getMyTickets(
            @RequestParam(value = "userEmail", required = false) String userEmail,
            Authentication authentication) {

        String targetEmail = resolveUserEmail(userEmail, authentication);

        if (targetEmail == null || targetEmail.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.badRequest("Pengguna tidak terautentikasi atau email tidak ditemukan"));
        }

        List<TicketItem> tickets = ticketService.getTicketsByEmail(targetEmail);
        return ResponseEntity.ok(ApiResponse.success("Daftar tiket user", tickets));
    }

    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<ApiResponse<List<TicketDetailResponse>>> getTicketsByOrder(
            @PathVariable String orderId) {
        try {
            UUID orderUuid = UUID.fromString(orderId.trim());
            List<TicketDetailResponse> tickets = ticketService.getTicketsByOrderId(orderUuid);
            return ResponseEntity.ok(ApiResponse.success("Daftar tiket order berhasil dimuat", tickets));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("Format Order ID tidak valid"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.badRequest("Gagal memuat tiket: " + e.getMessage()));
        }
    }

    // Anti-IDOR: Validasi pemilik tiket atau role Panitia/Admin
    @GetMapping("/issued-detail")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> getIssuedDetail(
            @RequestParam String ticketCode,
            @RequestParam(value = "userEmail", required = false) String userEmail,
            Authentication authentication) {
        try {
            TicketDetailResponse detail = ticketService.getIssuedDetail(ticketCode);

            String currentUser = resolveUserEmail(userEmail, authentication);

            if (currentUser != null && !currentUser.isBlank()) {

                // Pengecekan pemilik tiket: cocokkan dengan email peserta atau email pembeli
                boolean isOwner = currentUser.equalsIgnoreCase(detail.getAttendeeEmail())
                        || (detail.getCustomerEmail() != null
                                && currentUser.equalsIgnoreCase(detail.getCustomerEmail()));

                // Pengecekan role Staff / Admin (akomodasi format 'ROLE_ADMIN' maupun 'ADMIN')
                boolean isStaff = authentication != null && authentication.getAuthorities() != null
                        && authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")
                                        || a.getAuthority().equalsIgnoreCase("ROLE_ORGANIZER")
                                        || a.getAuthority().equalsIgnoreCase("ADMIN")
                                        || a.getAuthority().equalsIgnoreCase("ORGANIZER"));

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

    // Scan tiket di venue
    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> scanTicket(@RequestBody Map<String, String> payload) {
        String ticketCode = payload.get("ticketCode");
        if (ticketCode == null || ticketCode.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("Kode tiket tidak boleh kosong"));
        }
        try {
            String result = ticketService.validateAndUseTicket(ticketCode);
            return ResponseEntity.ok(ApiResponse.success("Proses scan selesai",
                    Map.of("status", result, "message", "Proses scan selesai")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * Helper method standar Spring Security untuk mengambil identitas
     * pengguna/email
     */
    private String resolveUserEmail(String paramEmail, Authentication authentication) {
        if (paramEmail != null && !paramEmail.isBlank()) {
            return paramEmail;
        }

        if (authentication != null) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof UserDetails userDetails) {
                return userDetails.getUsername();
            }

            return authentication.getName();
        }

        return null;
    }
}