package com.example.eventday.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsResponse {
    private String settingKey;
    private String settingValue;
    private String description;
}