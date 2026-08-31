package com.example.eventday.controller;

import com.example.eventday.dto.RescheduleRequestDto;
import com.example.eventday.entity.RescheduleRequest;
import com.example.eventday.service.RescheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reschedules")
@RequiredArgsConstructor
public class RescheduleController {

    private final RescheduleService rescheduleService;

    // Endpoint EO mengajukan jadwal baru
    @PostMapping
    public ResponseEntity<RescheduleRequest> createReschedule(@RequestBody RescheduleRequestDto dto) {
        return ResponseEntity.ok(rescheduleService.createRescheduleRequest(dto));
    }

    // Endpoint Super Admin meloloskan/ACC reschedule
    @PutMapping("/{rescheduleId}/approve")
    public ResponseEntity<RescheduleRequest> approveReschedule(@PathVariable UUID rescheduleId) {
        return ResponseEntity.ok(rescheduleService.approveReschedule(rescheduleId));
    }

    @PutMapping("/{rescheduleId}/reject")
    public ResponseEntity<RescheduleRequest> rejectReschedule(@PathVariable UUID rescheduleId) {
        return ResponseEntity.ok(rescheduleService.rejectReschedule(rescheduleId));
    }

    @GetMapping
    public ResponseEntity<List<RescheduleRequest>> getAllRequests() {
        return ResponseEntity.ok(rescheduleService.getAllRequests());
    }
}