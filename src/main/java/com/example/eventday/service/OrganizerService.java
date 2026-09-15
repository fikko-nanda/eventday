package com.example.eventday.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Service
public class OrganizerService {

    // ==========================================
    // SPEC V1.3.0 (MODUL 07)
    // ==========================================

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

    // ==========================================
    // PROFIL & LEGALITAS EO
    // ==========================================

    public Map<String, Object> getProfile() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", "PT Harmoni Musik Indonesia");
        profile.put("pic_name", "Budi Santoso");
        profile.put("email", "budi.santoso@harmoni.co.id");
        profile.put("phone", "+6281234567890");
        profile.put("npwp", "01.234.567.8-901.000");
        profile.put("avatar_url", "https://api.dicebear.com/7.x/identicon/svg?seed=harmoni");
        return profile;
    }

    public Map<String, Object> updateProfile(Map<String, Object> payload) {
        return payload;
    }

    public Map<String, Object> uploadAvatar(MultipartFile file) {
        Map<String, Object> data = new HashMap<>();
        data.put("avatar_url", "https://storage.eventday.id/avatars/" + file.getOriginalFilename());
        return data;
    }

    public Map<String, Object> uploadPortfolio(MultipartFile file) {
        Map<String, Object> data = new HashMap<>();
        data.put("portfolio_url", "https://storage.eventday.id/docs/" + file.getOriginalFilename());
        return data;
    }

    public Map<String, Object> uploadDeed(MultipartFile file) {
        Map<String, Object> data = new HashMap<>();
        data.put("company_deed_url", "https://storage.eventday.id/docs/" + file.getOriginalFilename());
        return data;
    }

    public Map<String, Object> getProfileDocuments() {
        Map<String, Object> docs = new HashMap<>();
        docs.put("portfolio_name", "CV_Portofolio.pdf");
        docs.put("deed_name", "Akta_Perusahaan.pdf");
        docs.put("ktp_name", "KTP_PIC.jpg");
        return docs;
    }

    // ==========================================
    // AUTENTIKASI EO
    // ==========================================

    public void changePassword(Map<String, Object> payload) {
        // Business logic ubah password dihandle di sini
    }

    public void logout() {
        // Business logic invalidasi token/sesi dihandle di sini
    }
}