package com.example.eventday.controller.organizer;

import org.springframework.web.multipart.MultipartFile;
import com.example.eventday.dto.ApiResponse;
import com.example.eventday.service.organizer.OrganizerEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizer/events")
@RequiredArgsConstructor
public class OrganizerEventController {

    private final OrganizerEventService eventService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOrganizerEvents() {
        return ResponseEntity.ok(ApiResponse.ok("Daftar event organizer berhasil diambil", eventService.getOrganizerEvents()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> createEventMultipart(
            @RequestPart(value = "event", required = false) Map<String, Object> event,
            @RequestPart(value = "data", required = false) Map<String, Object> data,
            @RequestParam(value = "event", required = false) String eventJson,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        Map<String, Object> request = event != null ? event : data;
        if (request == null && eventJson != null && !eventJson.isBlank()) {
            try {
                request = new com.fasterxml.jackson.databind.ObjectMapper().readValue(eventJson,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Format JSON event tidak valid: " + e.getMessage());
            }
        }
        if (request == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Part 'event' wajib diisi (kirim sebagai part 'event' atau 'data')");
        }
        return ResponseEntity.status(201).body(ApiResponse.ok("Event berhasil dibuat", eventService.createEvent(request, file)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> createEventJson(
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body(ApiResponse.ok("Event berhasil dibuat", eventService.createEvent(request, null)));
    }

    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateEvent(
            @RequestPart("event") Map<String, Object> request,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok("Event berhasil diperbarui", eventService.updateEvent(request, file)));
    }

    @PostMapping("/publish")
    public ResponseEntity<ApiResponse<Map<String, Object>>> publishEvent(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ApiResponse.ok("Event berhasil dipublikasikan", eventService.publishEvent(request)));
    }

    @GetMapping("/draft")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDraftEvents() {
        return ResponseEntity.ok(ApiResponse.ok("Daftar draft event berhasil diambil", eventService.getDraftEvents()));
    }

    @GetMapping("/{id}/sales-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEventSalesSummary(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Ringkasan penjualan event berhasil diambil", eventService.getEventSalesSummary(id)));
    }

    @PostMapping("/banner")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadBanner(@RequestParam("file") MultipartFile file) {
        Map<String, Object> data = eventService.uploadBanner(file);
        return ResponseEntity.ok(ApiResponse.ok("Banner event berhasil diunggah", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEventDetail(@PathVariable("id") UUID eventId) {
        return ResponseEntity.ok(ApiResponse.ok("Detail event berhasil diambil", eventService.getEventDetailById(eventId)));
    }
}