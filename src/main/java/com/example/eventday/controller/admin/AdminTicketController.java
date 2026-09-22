package com.example.eventday.controller.admin;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.dto.admin.AdminTicketGenerateRequest;
import com.example.eventday.dto.admin.AdminTicketListResponse;
import com.example.eventday.dto.admin.AdminTicketResponse;
import com.example.eventday.service.admin.AdminTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/admin/tickets", "/api/admin/tickets"})
@RequiredArgsConstructor
public class AdminTicketController {

    private final AdminTicketService adminTicketService;

    private UUID getAdminId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    @GetMapping
    public ApiResponse<Page<AdminTicketListResponse>> getTickets(
            @RequestParam(value = "eventId", required = false) UUID eventId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam(value = "dateFrom", required = false) LocalDateTime dateFrom,
            @RequestParam(value = "dateTo", required = false) LocalDateTime dateTo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.ok("Berhasil mengambil daftar tiket",
                adminTicketService.getTickets(eventId, status, userId, dateFrom, dateTo, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminTicketResponse> getTicketDetail(@PathVariable("id") UUID ticketItemId) {
        return ApiResponse.ok("Berhasil mengambil detail tiket", adminTicketService.getTicketDetail(ticketItemId));
    }

    @PostMapping("/generate")
    public ApiResponse<List<AdminTicketResponse>> generateTickets(
            @Valid @RequestBody AdminTicketGenerateRequest request,
            Authentication authentication) {
        return ApiResponse.created("Tiket berhasil digenerate",
                adminTicketService.generateTickets(request.getOrderId(), getAdminId(authentication)));
    }

    @PostMapping("/{id}/revoke")
    public ApiResponse<String> revokeTicket(
            @PathVariable("id") UUID ticketItemId,
            Authentication authentication) {
        adminTicketService.revokeTicket(ticketItemId, getAdminId(authentication));
        return ApiResponse.ok("Tiket berhasil di-revoke", null);
    }

    @PostMapping("/{id}/checkin")
    public ApiResponse<String> checkInTicket(
            @PathVariable("id") UUID ticketItemId,
            Authentication authentication) {
        adminTicketService.checkInTicket(ticketItemId, getAdminId(authentication));
        return ApiResponse.ok("Tiket berhasil di-check-in", null);
    }

    @GetMapping("/inventory")
    public ApiResponse<Map<String, Object>> getEventTicketInventory(
            @RequestParam(value = "eventId", required = true) UUID eventId) {
        return ApiResponse.ok("Berhasil mengambil inventaris tiket", adminTicketService.getEventTicketInventory(eventId));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTicketsCsv(
            @RequestParam(value = "eventId", required = false) UUID eventId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "dateFrom", required = false) LocalDateTime dateFrom,
            @RequestParam(value = "dateTo", required = false) LocalDateTime dateTo) {
        byte[] csv = adminTicketService.exportTicketsCsv(eventId, status, dateFrom, dateTo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tickets.csv\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }
}
