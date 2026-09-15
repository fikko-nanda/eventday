package com.example.eventday.service.admin;

import com.example.eventday.dto.admin.AdminSettingsRequest;
import com.example.eventday.entity.AuditLog;
import com.example.eventday.entity.Settings;
import com.example.eventday.repository.AuditLogRepository;
import com.example.eventday.repository.SettingsRepository;
import com.example.eventday.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminSettingsService {

    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(int page, int size) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Map<String, String> getGeneralSettings() {
        List<Settings> all = settingsRepository.findAll();
        Map<String, String> map = new LinkedHashMap<>();
        for (Settings s : all) {
            map.put(s.getSettingsKey(), s.getSettingsValue());
        }
        return map;
    }

    @Transactional
    public void saveGeneralSettings(AdminSettingsRequest req, UUID adminId) {
        saveOrUpdate("APP_NAME", req.getAppName());
        saveOrUpdate("CONTACT_EMAIL", req.getContactEmail());
        if (req.getAdminFee() != null) {
            saveOrUpdate("ADMIN_FEE", String.valueOf(req.getAdminFee()));
        }
        if (req.getOrderExpiryMinutes() != null) {
            saveOrUpdate("ORDER_EXPIRY_MINUTES", String.valueOf(req.getOrderExpiryMinutes()));
        }

        auditLogService.log(adminId, "SUPERADMIN", "UPDATE_SETTINGS", "Memperbarui konfigurasi general sistem");
    }

    private void saveOrUpdate(String key, String value) {
        if (value == null) return;
        Settings s = settingsRepository.findBySettingsKey(key)
                .orElse(Settings.builder().settingsKey(key).build());
        s.setSettingsValue(value);
        settingsRepository.save(s);
    }
}