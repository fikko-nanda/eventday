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
public class EventDetailResponse {

    private String id;
    private String title;
    private String category;
    private String categoryLabel;
    private LocalDateTime date;
    private String dateDisplay;
    private String location;
    private String description;
    private String image;
    private String status;
    private String statusLabel;
    private List<String> facilities;
    private List<LineupItem> lineup;
    private List<TicketItem> tickets;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineupItem {
        private String name;
        private String image;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketItem {
        private String id;
        private String name;
        private String label;
        private BigDecimal price;
        private String priceDisplay;
        private Integer quota;
        private Integer remaining;
        private LocalDateTime saleStart;
        private LocalDateTime saleEnd;
    }
}
