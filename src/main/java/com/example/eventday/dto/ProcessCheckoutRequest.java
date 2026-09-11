package com.example.eventday.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class ProcessCheckoutRequest {
    private UUID orderId;
}