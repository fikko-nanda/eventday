package com.example.eventday.service;

import com.example.eventday.dto.SettingsResponse;
import com.example.eventday.entity.Settings;
import com.example.eventday.repository.SettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SettingsRepository settingsRepository;
    private final AuditLogService auditLogService;

    @PostConstruct
    public void initDefaultSettings() {
        createIfNotExists("ADMIN_FEE", "5000", "Admin fee per order (Rp)");
        createIfNotExists("ORDER_EXPIRY_MINUTES", "15", "Order expiry time in minutes");
    }

    private void createIfNotExists(String key, String defaultValue, String description) {
        if (settingsRepository.findBySettingKey(key).isEmpty()) {
            Settings setting = Settings.builder()
                    .settingKey(key)
                    .settingValue(defaultValue)
                    .description(description)
                    .build();
            settingsRepository.save(setting);
        }
    }

    public List<SettingsResponse> getAllSettings() {
        return settingsRepository.findAll()
                .stream()
                .map(s -> SettingsResponse.builder()
                        .settingKey(s.getSettingKey())
                        .settingValue(s.getSettingValue())
                        .description(s.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    public SettingsResponse getSetting(String key) {
        Settings setting = settingsRepository.findBySettingKey(key)
                .orElseThrow(() -> new RuntimeException("Setting tidak ditemukan: " + key));
        return SettingsResponse.builder()
                .settingKey(setting.getSettingKey())
                .settingValue(setting.getSettingValue())
                .description(setting.getDescription())
                .build();
    }

    public BigDecimal getAdminFee() {
        Settings setting = settingsRepository.findBySettingKey("ADMIN_FEE")
                .orElseThrow(() -> new RuntimeException("Setting ADMIN_FEE tidak ditemukan!"));
        return new BigDecimal(setting.getSettingValue());
    }

    public int getExpiryMinutes() {
        Settings setting = settingsRepository.findBySettingKey("ORDER_EXPIRY_MINUTES")
                .orElseThrow(() -> new RuntimeException("Setting ORDER_EXPIRY_MINUTES tidak ditemukan!"));
        return Integer.parseInt(setting.getSettingValue());
    }

    public SettingsResponse updateSetting(String key, String value, String description) {
        Settings setting = settingsRepository.findBySettingKey(key)
                .orElseThrow(() -> new RuntimeException("Setting tidak ditemukan: " + key));

        setting.setSettingValue(value);
        if (description != null) {
            setting.setDescription(description);
        }

        Settings saved = settingsRepository.save(setting);

        auditLogService.log(null, "Admin", "UPDATE", "SETTING", key, "Ubah setting " + key + " ke " + value);

        return SettingsResponse.builder()
                .settingKey(saved.getSettingKey())
                .settingValue(saved.getSettingValue())
                .description(saved.getDescription())
                .build();
    }
}
