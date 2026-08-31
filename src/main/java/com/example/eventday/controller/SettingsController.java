package com.example.eventday.controller;

import com.example.eventday.entity.Settings;
import com.example.eventday.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    public ResponseEntity<List<Settings>> getAllSettings() {
        return ResponseEntity.ok(settingsService.getAllSettings());
    }

    @GetMapping("/{key}")
    public ResponseEntity<?> getSetting(@PathVariable String key) {
        try {
            return ResponseEntity.ok(settingsService.getSetting(key));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{key}")
    public ResponseEntity<?> updateSetting(
            @PathVariable String key,
            @RequestBody Map<String, String> body
    ) {
        try {
            String value = body.get("value");
            String description = body.get("description");
            return ResponseEntity.ok(settingsService.updateSetting(key, value, description));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
