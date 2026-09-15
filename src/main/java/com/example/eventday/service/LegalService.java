package com.example.eventday.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class LegalService {

    public Map<String, Object> getTermsAndConditions() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Syarat dan Ketentuan EventDay");
        data.put("content", "Selamat datang di EventDay. Dengan mengakses platform ini, Anda menyetujui seluruh syarat dan ketentuan yang berlaku.");
        data.put("updated_at", "2026-09-14");
        return data;
    }

    public Map<String, Object> getPrivacyPolicy() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Kebijakan Privasi EventDay");
        data.put("content", "EventDay berkomitmen penuh melindungi kerahasiaan data dan privasi setiap pengguna.");
        data.put("updated_at", "2026-09-14");
        return data;
    }
}