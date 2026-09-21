package com.example.eventday.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminEventRequest {

    @NotBlank(message = "Judul event wajib diisi")
    private String title;

    private String description;

    @NotBlank(message = "Kategori event wajib diisi")
    private String category;

    @NotBlank(message = "Nama venue wajib diisi")
    private String venueName;

    @NotNull(message = "Tanggal mulai wajib diisi")
    private LocalDateTime startDate;

    @NotNull(message = "Tanggal selesai wajib diisi")
    private LocalDateTime endDate;

    private String bannerUrl;

    private String facility;

    private String lineup;

    private Boolean isFeatured;

    private List<TierItem> ticketTiers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TierItem {
        @NotBlank(message = "Nama tier wajib diisi")
        private String tierName;

        @NotNull(message = "Harga tier wajib diisi")
        @DecimalMin(value = "0", message = "Harga minimal 0")
        private BigDecimal price;

        @NotNull(message = "Kuota tier wajib diisi")
        @Min(value = 1, message = "Kuota minimal 1")
        private Integer totalQuota;
    }
}
