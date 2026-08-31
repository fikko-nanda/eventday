package com.example.eventday.controller;

import com.example.eventday.dto.AuditLogResponse;
import com.example.eventday.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> getAllLogs() {
        return ResponseEntity.ok(auditLogService.getAllLogs());
    }

    @GetMapping("/actor/{actorId}")
    public ResponseEntity<List<AuditLogResponse>> getLogsByActor(@PathVariable UUID actorId) {
        return ResponseEntity.ok(auditLogService.getLogsByActor(actorId));
    }
}