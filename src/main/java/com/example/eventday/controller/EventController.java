package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.dto.EventCatalogResponse;
import com.example.eventday.dto.EventDetailResponse;
import com.example.eventday.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<ApiResponse<EventCatalogResponse>> getEvents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "latest") String sort) {
        try {
            EventCatalogResponse data = eventService.getEvents(category, search, location, page, size, sort);
            return ResponseEntity.ok(ApiResponse.ok("Berhasil mengambil daftar event", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<EventCatalogResponse>> getFeaturedEvents() {
        try {
            EventCatalogResponse data = eventService.getFeaturedEvents();
            return ResponseEntity.ok(ApiResponse.ok("Berhasil mengambil event unggulan", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventDetailResponse>> getEventDetail(@PathVariable UUID id) {
        try {
            EventDetailResponse data = eventService.getEventDetail(id);
            return ResponseEntity.ok(ApiResponse.ok("Berhasil mengambil detail event", data));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.notFound(e.getMessage()));
        }
    }
}
