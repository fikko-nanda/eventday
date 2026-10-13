package com.example.eventday.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data datar untuk template email e-ticket.
 *
 * Sengaja TIDAK memakai entity JPA (TicketItem/Order/TicketTier) karena objek ini
 * diteruskan ke EmailService.sendTicketEmail() yang berjalan @Async di thread lain.
 * Entity JPA pada session yang sudah ditutup akan memicu LazyInitializationException.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketEmailData {

    private String orderNumber;
    private String eventTitle;
    private String eventDate;
    private String venueName;
    private String buyerName;
    private String recipientEmail;
    private String totalAmount;

    @Builder.Default
    private List<TicketLine> tickets = List.of();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketLine {
        private String ticketCode;
        private String attendeeName;
        private String tierName;
        private String priceDisplay;
    }
}
