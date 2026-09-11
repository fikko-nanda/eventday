package com.example.eventday.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendeesRequest {
    private UUID orderId;
    private List<AttendeeDetail> attendees;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendeeDetail {
        private String fullName;
        private String email;
        private String phoneNumber;
        private String identityCardNumber;
    }
}