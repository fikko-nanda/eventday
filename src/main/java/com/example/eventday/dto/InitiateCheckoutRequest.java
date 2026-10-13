package com.example.eventday.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateCheckoutRequest {
    @NotNull(message = "Tier ID wajib diisi")
    private UUID tierId;

    @NotNull(message = "Quantity wajib diisi")
    @Min(value = 1, message = "Quantity minimal 1")
    private Integer quantity;
}
