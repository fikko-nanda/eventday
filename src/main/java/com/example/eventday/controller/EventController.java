package com.example.eventday.controller;

import com.example.eventday.dto.CreateEventRequest;
import com.example.eventday.dto.EventResponse;
import com.example.eventday.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<?> createEvent(@RequestBody CreateEventRequest request) {
        try {
            EventResponse response = eventService.createEvent(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllPublishedEvents());
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<?> getEventById(@PathVariable UUID eventId) {
        try {
            return ResponseEntity.ok(eventService.getEventById(eventId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}