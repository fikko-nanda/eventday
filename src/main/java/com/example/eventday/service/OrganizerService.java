package com.example.eventday.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Service
public class OrganizerService {

    public Map<String, Object> registerOrganizer(Map<String, Object> request) {
        Map<String, Object> data = new HashMap<>();
        data.put("organizer_name", request.getOrDefault("name", "PT Penyelenggara Event"));
        data.put("verification_status", "PENDING");
        return data;
    }

    public Map<String, Object> uploadDocument(MultipartFile file, String documentType) {
        Map<String, Object> data = new HashMap<>();
        data.put("document_type", documentType);
        data.put("file_name", file.getOriginalFilename());
        data.put("file_size", file.getSize());
        return data;
    }

    public Map<String, Object> getOrganizerStatus() {
        Map<String, Object> data = new HashMap<>();
        data.put("organizer_name", "PT Penyelenggara Event");
        data.put("verification_status", "PENDING");
        return data;
    }

    public Map<String, Object> getOrganizerDashboard() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("total_revenue", 42500000);
        metrics.put("active_events", 2);
        metrics.put("tickets_sold", 1248);
        return metrics;
    }
}