package com.example.eventday.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventCatalogResponse {

    private List<EventItem> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventItem {
        private String id;
        private String title;
        private String category;
        private String categoryLabel;
        private LocalDateTime date;
        private String dateDisplay;
        private String time;
        private String location;
        private BigDecimal price;
        private String priceDisplay;
        private String image;
        private String status;
        private Boolean isFeatured;
    }
}
