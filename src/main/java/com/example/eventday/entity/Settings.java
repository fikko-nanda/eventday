package com.example.eventday.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settings {

    @Id
    @Column(name = "setting_key", length = 100)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    @Column(columnDefinition = "TEXT")
    private String description;

    public enum SettingKey {
        ADMIN_FEE,
        ORDER_EXPIRY_MINUTES
    }
}
