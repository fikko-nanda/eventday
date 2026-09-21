package com.example.eventday.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminTransactionStatusRequest {

    @NotBlank(message = "Status tidak boleh kosong")
    private String status; // PAID, REFUNDED, CANCELLED, WAITING_PAYMENT

    private String adminNote;
}
