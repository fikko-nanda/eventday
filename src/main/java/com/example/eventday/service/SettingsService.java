package com.example.eventday.service;

import com.example.eventday.entity.Settings;
import com.example.eventday.repository.SettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SettingsRepository settingsRepository;

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

    public List<Settings> getAllSettings() {
        return settingsRepository.findAll();
    }

    public Settings getSetting(String key) {
        return settingsRepository.findBySettingKey(key)
                .orElseThrow(() -> new RuntimeException("Setting tidak ditemukan: " + key));
    }

    public BigDecimal getAdminFee() {
        Settings setting = getSetting("ADMIN_FEE");
        return new BigDecimal(setting.getSettingValue());
    }

    public int getExpiryMinutes() {
        Settings setting = getSetting("ORDER_EXPIRY_MINUTES");
        return Integer.parseInt(setting.getSettingValue());
    }

    public Settings updateSetting(String key, String value, String description) {
        Settings setting = settingsRepository.findBySettingKey(key)
                .orElseThrow(() -> new RuntimeException("Setting tidak ditemukan: " + key));

        setting.setSettingValue(value);
        if (description != null) {
            setting.setDescription(description);
        }

        return settingsRepository.save(setting);
    }
}
