package com.example.eventday.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AdminTicketGenerateRequest {

    @NotNull(message = "Order ID wajib diisi")
    private UUID orderId;
}
