package com.example.eventday.dto;

import lombok.Data;
import java.util.List;

@Data
public class AttendeeRequest {
    private String orderId;
    private List<AttendeeItem> attendees;

    @Data
    public static class AttendeeItem {
        private String fullName;
        private String email;
        private String phoneNumber;
        private String identityNumber;
    }
}