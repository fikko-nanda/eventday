package com.example.eventday.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CreateEventRequest {
    @NotNull(message = "Organizer ID tidak boleh kosong")
    private UUID organizerId;

    @NotBlank(message = "Judul tidak boleh kosong")
    @Size(max = 150, message = "Judul maksimal 150 karakter")
    private String title;

    private String description;

    @Size(max = 50, message = "Kategori maksimal 50 karakter")
    private String category;

    @Size(max = 150, message = "Nama venue maksimal 150 karakter")
    private String venueName;

    private String bannerUrl;

    @NotNull(message = "Tanggal mulai tidak boleh kosong")
    private LocalDateTime startDate;

    @NotNull(message = "Tanggal selesai tidak boleh kosong")
    private LocalDateTime endDate;

    private List<String> facility;
}
