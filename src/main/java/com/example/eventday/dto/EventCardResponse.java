package com.example.eventday.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventCardResponse {
    private String id;
    private String title;
    private String posterUrl;
    private String location;
    private String category;
    private String categoryLabel;
    private LocalDateTime startDate;
    private String dateDisplay;
    private BigDecimal lowestPrice;
    private String priceDisplay;
}
