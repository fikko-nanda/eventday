package com.example.eventday.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class CreateOrderRequest {
    private UUID customerId;
    private UUID eventId;
    private UUID tierId;
    private List<AttendeeRequest> attendees;

    @Data
    public static class AttendeeRequest {
        private String name;
        private String nik;
    }
}