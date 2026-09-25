package com.example.eventday.controller.organizer;

import org.springframework.web.multipart.MultipartFile;
import com.example.eventday.dto.ApiResponse;
import com.example.eventday.service.organizer.OrganizerEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format JSON event tidak valid: " + e.getMessage());
            }
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Part 'event' wajib diisi (kirim sebagai part 'event' atau 'data')");
        }
        return ResponseEntity.status(201).body(ApiResponse.ok("Event berhasil dibuat", eventService.createEvent(request, file)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> createEventJson(
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body(ApiResponse.ok("Event berhasil dibuat", eventService.createEvent(request, null)));
    }

    // ===== UPDATE EVENT — dual route: frontend baru /update/{eventId} + fallback lama =====
    @PutMapping(value = {
            "/update/{eventId}",
            "/update",
            "/{eventId}"
    }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateEventMultipart(
            @PathVariable(value = "eventId", required = false) UUID pathEventId,
            @RequestParam(value = "id", required = false) UUID queryEventId,
            @RequestPart(value = "event", required = false) Map<String, Object> event,
            @RequestPart(value = "data", required = false) Map<String, Object> data,
            @RequestParam(value = "eventJson", required = false) String eventJson,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        Map<String, Object> request = event != null ? event : data;
        if (request == null && eventJson != null && !eventJson.isBlank()) {
            try {
                request = new com.fasterxml.jackson.databind.ObjectMapper().readValue(eventJson,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format JSON event tidak valid: " + e.getMessage());
            }
        }
        if (request == null) {
            request = new java.util.HashMap<>();
        }

        UUID id = resolveEventId(pathEventId, queryEventId, request);
        return ResponseEntity.ok(ApiResponse.ok("Event berhasil diperbarui",
                eventService.updateEvent(id, request, file)));
    }

    @PutMapping(value = {
            "/update/{eventId}",
            "/update",
            "/{eventId}"
    }, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateEventJson(
            @PathVariable(value = "eventId", required = false) UUID pathEventId,
            @RequestParam(value = "id", required = false) UUID queryEventId,
            @RequestBody(required = false) Map<String, Object> request) {

        Map<String, Object> body = request != null ? request : new java.util.HashMap<>();
        UUID id = resolveEventId(pathEventId, queryEventId, body);
        return ResponseEntity.ok(ApiResponse.ok("Event berhasil diperbarui",
                eventService.updateEvent(id, body, null)));
    }

    // Prioritas ID: path {eventId} → query ?id= → field payload eventId/id/event_id
    private UUID resolveEventId(UUID pathEventId, UUID queryEventId, Map<String, Object> body) {
        UUID id = pathEventId != null ? pathEventId : queryEventId;
        if (id == null) {
            for (String key : new String[]{"eventId", "event_id", "id"}) {
                Object v = body.get(key);
                if (v != null && !String.valueOf(v).isBlank() && !"null".equals(String.valueOf(v))) {
                    try {
                        id = UUID.fromString(String.valueOf(v).trim());
                        break;
                    } catch (IllegalArgumentException ignored) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format eventId tidak valid: " + v);
                    }
                }
            }
        }
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "eventId diperlukan (path /update/{eventId}, query ?id=, atau field payload)");
        }
        return id;
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