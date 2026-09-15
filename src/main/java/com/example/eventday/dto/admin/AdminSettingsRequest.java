package com.example.eventday.dto.admin;

import lombok.Data;

@Data
public class AdminSettingsRequest {
    private String appName;
    private String contactEmail;
    private Integer adminFee;
    private Integer orderExpiryMinutes;
}