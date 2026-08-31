package com.example.eventday.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateOrderRequest {
    @NotNull(message = "Customer ID tidak boleh kosong")
    private UUID customerId;

    @NotNull(message = "Event ID tidak boleh kosong")
    private UUID eventId;

    @NotNull(message = "Tier ID tidak boleh kosong")
    private UUID tierId;

    @NotEmpty(message = "Attendees tidak boleh kosong")
    @Valid
    private List<AttendeeRequest> attendees;

    @Data
    public static class AttendeeRequest {
        @jakarta.validation.constraints.NotBlank(message = "Nama attendee tidak boleh kosong")
        @jakarta.validation.constraints.Size(min = 1, max = 100, message = "Nama attendee maksimal 100 karakter")
        private String name;

        @jakarta.validation.constraints.NotBlank(message = "NIK attendee tidak boleh kosong")
        @jakarta.validation.constraints.Pattern(regexp = "\\d{16}", message = "NIK harus 16 digit angka")
        private String nik;
    }
}
