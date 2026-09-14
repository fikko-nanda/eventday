package com.example.eventday.service;

import com.example.eventday.dto.TicketDetailResponse;
import com.example.eventday.entity.Order;
import com.example.eventday.entity.TicketItem;
import com.example.eventday.entity.TicketTier;
import com.example.eventday.repository.TicketItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketItemRepository ticketItemRepository;

    @Transactional
    public TicketItem generateTicket(Order order, TicketTier tier, String attendeeName, String attendeeEmail, String attendeeNik) {
        TicketItem ticketItem = TicketItem.builder()
                .order(order)
                .tier(tier)
                .attendeeName(attendeeName)
                .attendeeEmail(attendeeEmail)
                .attendeeNik(attendeeNik)
                .checkInStatus("UNREDEEMED")
                .build();

        return ticketItemRepository.save(ticketItem);
    }

    public List<TicketItem> getTicketsByEmail(String email) {
        return ticketItemRepository.findByOrderCustomerEmailOrderByCreatedAtDesc(email);
    }

    // Endpoint query data detail E-Ticket berdasarkan UUID kode tiket (/tickets/issued-detail)
    @Transactional(readOnly = true)
    public TicketDetailResponse getIssuedDetail(String ticketCode) {
        UUID ticketItemId;
        try {
            ticketItemId = UUID.fromString(ticketCode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Format kode tiket tidak valid!");
        }

        TicketItem ticket = ticketItemRepository.findById(ticketItemId)
                .orElseThrow(() -> new IllegalArgumentException("Tiket tidak ditemukan!"));

        Order order = ticket.getOrder();
        TicketTier tier = ticket.getTier();

        String eventTitle = null;
        LocalDateTime eventDate = null;
        String venueName = null;
        String categoryName = null;

        if (tier != null) {
            categoryName = tier.getTierName();
            if (tier.getEvent() != null) {
                eventTitle = tier.getEvent().getTitle();
                eventDate = tier.getEvent().getStartDate(); // Disesuaikan dengan Event.java (getStartDate)
                venueName = tier.getEvent().getVenueName();
            }
        }

        return TicketDetailResponse.builder()
                .ticketId(ticket.getTicketItemId().toString())
                .ticketCode(ticket.getTicketItemId().toString())
                .orderId(order != null && order.getOrderId() != null ? order.getOrderId().toString() : null)
                .eventTitle(eventTitle)
                .eventDate(eventDate)
                .venueName(venueName)
                .categoryName(categoryName)
                .attendeeName(ticket.getAttendeeName())
                .attendeeEmail(ticket.getAttendeeEmail())
                .attendeeIdentityNumber(ticket.getAttendeeNik())
                .status(ticket.getCheckInStatus())
                .issuedAt(ticket.getCreatedAt() != null ? ticket.getCreatedAt() : LocalDateTime.now())
                .build();
    }

    // Dipakai saat panitia me-scan QR Code di venue (ticketCode berupa UUID ticketItemId)
    @Transactional
    public String validateAndUseTicket(String ticketCode) {
        UUID ticketItemId;
        try {
            ticketItemId = UUID.fromString(ticketCode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Format kode tiket tidak valid!");
        }

        TicketItem ticket = ticketItemRepository.findById(ticketItemId)
                .orElseThrow(() -> new IllegalArgumentException("Tiket tidak ditemukan!"));

        if ("CHECKED_IN".equalsIgnoreCase(ticket.getCheckInStatus())) {
            return "TIKET_SUDAH_DIPAKAI";
        }

        ticket.setCheckInStatus("CHECKED_IN");
        ticket.setCheckInAt(LocalDateTime.now());
        ticketItemRepository.save(ticket);
        return "TIKET_VALID";
    }
}