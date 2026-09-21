package com.example.eventday.controller.admin;

import com.example.eventday.dto.CreateEventRequest;
import com.example.eventday.dto.ApiResponse;
import com.example.eventday.dto.admin.*;
import com.example.eventday.service.admin.AdminEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping({"/admin/events", "/api/admin/events"})
@RequiredArgsConstructor
public class AdminEventController {

    private final AdminEventService adminEventService;

    private UUID getAdminId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    @GetMapping
    public ApiResponse<Page<AdminEventListResponse>> getEvents(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "organizerId", required = false) UUID organizerId,
            @RequestParam(value = "dateFrom", required = false) LocalDateTime dateFrom,
            @RequestParam(value = "dateTo", required = false) LocalDateTime dateTo,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.ok("Berhasil mengambil daftar event",
                adminEventService.getEvents(status, category, organizerId, dateFrom, dateTo, search, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminEventResponse> getEventDetail(@PathVariable("id") UUID eventId) {
        return ApiResponse.ok("Berhasil mengambil detail event", adminEventService.getEventDetail(eventId));
    }

    @PostMapping
    public ApiResponse<AdminEventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            Authentication authentication) {
        return ApiResponse.created("Event berhasil dibuat",
                adminEventService.createEvent(request, getAdminId(authentication)));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminEventResponse> updateEvent(
            @PathVariable("id") UUID eventId,
            @Valid @RequestBody CreateEventRequest request,
            Authentication authentication) {
        return ApiResponse.ok("Event berhasil diperbarui",
                adminEventService.updateEvent(eventId, request, getAdminId(authentication)));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<String> updateEventStatus(
            @PathVariable("id") UUID eventId,
            @Valid @RequestBody AdminEventStatusRequest request,
            Authentication authentication) {
        adminEventService.updateEventStatus(eventId, request.getStatus(), getAdminId(authentication));
        return ApiResponse.ok("Status event berhasil diperbarui", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteEvent(
            @PathVariable("id") UUID eventId,
            Authentication authentication) {
        adminEventService.deleteEvent(eventId, getAdminId(authentication));
        return ApiResponse.ok("Event berhasil dihapus", null);
    }

    @GetMapping("/{id}/sales")
    public ApiResponse<AdminEventSalesResponse> getEventSales(@PathVariable("id") UUID eventId) {
        return ApiResponse.ok("Berhasil mengambil ringkasan penjualan event", adminEventService.getEventSales(eventId));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportEventsCsv(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "organizerId", required = false) UUID organizerId,
            @RequestParam(value = "dateFrom", required = false) LocalDateTime dateFrom,
            @RequestParam(value = "dateTo", required = false) LocalDateTime dateTo,
            @RequestParam(value = "search", required = false) String search) {
        byte[] csv = adminEventService.exportEventsCsv(status, category, organizerId, dateFrom, dateTo, search);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"events.csv\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }
}
