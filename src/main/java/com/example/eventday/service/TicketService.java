package com.example.eventday.service;

import com.example.eventday.dto.TicketDetailResponse;
import com.example.eventday.entity.Event;
import com.example.eventday.entity.Order;
import com.example.eventday.entity.TicketItem;
import com.example.eventday.entity.TicketTier;
import com.example.eventday.model.Attendee;
import com.example.eventday.repository.AttendeeRepository;
import com.example.eventday.repository.TicketItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class TicketService {

    private final TicketItemRepository ticketItemRepository;
    private final AttendeeRepository attendeeRepository;

    @Transactional
    public List<TicketItem> generateTicketsForOrder(Order order) {
        if (order == null || order.getOrderId() == null) {
            throw new IllegalArgumentException("Order dan Order ID tidak boleh kosong");
        }

        // Idempotency Check
        List<TicketItem> existingTickets = ticketItemRepository.findByOrderOrderId(order.getOrderId());
        if (existingTickets != null && !existingTickets.isEmpty()) {
            return existingTickets;
        }

        String orderIdStr = order.getOrderId().toString();
        List<Attendee> attendees = attendeeRepository.findByOrderId(orderIdStr);
        List<TicketItem> generatedTickets = new ArrayList<>();
        TicketTier tier = order.getTicketTier();

        if (attendees != null && !attendees.isEmpty()) {
            for (Attendee attendee : attendees) {
                TicketItem ticket = generateTicket(
                        order,
                        tier,
                        attendee.getFullName(),
                        attendee.getEmail(),
                        attendee.getIdentityNumber());
                generatedTickets.add(ticket);
            }
        } else {
            String email = (order.getCustomer() != null && order.getCustomer().getEmail() != null)
                    ? order.getCustomer().getEmail()
                    : "";

            int qty = order.getQuantity() != null ? order.getQuantity() : 1;

            for (int i = 0; i < qty; i++) {
                TicketItem ticket = generateTicket(
                        order,
                        tier,
                        "Pemegang Tiket " + (i + 1),
                        email,
                        null);
                generatedTickets.add(ticket);
            }
        }
        return generatedTickets;
    }

    @Transactional
    public TicketItem generateTicket(Order order, TicketTier tier, String attendeeName, String attendeeEmail,
            String attendeeNik) {
        TicketItem ticketItem = TicketItem.builder()
                .order(order)
                .tier(tier)
                .attendeeName(attendeeName)
                .attendeeEmail(attendeeEmail)
                .attendeeNik(attendeeNik)
                .checkInStatus("UNREDEEMED")
                .createdAt(LocalDateTime.now())
                .build();

        return ticketItemRepository.save(ticketItem);
    }

    @Transactional(readOnly = true)
    public List<TicketItem> getTicketsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }
        List<TicketItem> tickets = ticketItemRepository
                .findByAttendeeEmailIgnoreCaseOrOrderCustomerEmailIgnoreCaseOrderByCreatedAtDesc(email, email);

        if (tickets != null) {
            tickets.forEach(ticket -> {
                if (ticket.getTier() != null && ticket.getTier().getEvent() != null) {
                    ticket.getTier().getEvent().getTitle();
                }
            });
        }
        return tickets != null ? tickets : List.of();
    }

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

        String eventId = null;
        String eventImageUrl = null;
        String eventTitle = null;
        LocalDateTime eventDate = null;
        String venueName = null;
        String categoryName = null;
        String customerEmail = (order != null && order.getCustomer() != null) ? order.getCustomer().getEmail() : null;

        if (tier != null) {
            categoryName = tier.getTierName();
            Event event = tier.getEvent();
            if (event != null) {
                if (event.getEventId() != null) {
                    eventId = event.getEventId().toString();
                }
                eventImageUrl = event.getBannerUrl(); // Menggunakan getBannerUrl() dari Event.java
                eventTitle = event.getTitle();
                eventDate = event.getStartDate();
                venueName = event.getVenueName();
            }
        }

        return TicketDetailResponse.builder()
                .ticketId(ticket.getTicketItemId().toString())
                .ticketCode(ticket.getTicketItemId().toString())
                .orderId(order != null && order.getOrderId() != null ? order.getOrderId().toString() : null)
                .eventId(eventId)
                .eventImageUrl(eventImageUrl)
                .eventTitle(eventTitle)
                .eventDate(eventDate)
                .venueName(venueName)
                .categoryName(categoryName)
                .attendeeName(ticket.getAttendeeName())
                .attendeeEmail(ticket.getAttendeeEmail())
                .customerEmail(customerEmail)
                .attendeeIdentityNumber(ticket.getAttendeeNik())
                .status(ticket.getCheckInStatus())
                .issuedAt(ticket.getCreatedAt() != null ? ticket.getCreatedAt() : LocalDateTime.now())
                .build();
    }

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

        if ("REFUNDED".equalsIgnoreCase(ticket.getCheckInStatus())) {
            return "TIKET_SUDAH_DIREFUNDED";
        }

        if ("REVOKED".equalsIgnoreCase(ticket.getCheckInStatus())) {
            return "TIKET_DIREVOKE";
        }

        ticket.setCheckInStatus("CHECKED_IN");
        ticket.setCheckInAt(LocalDateTime.now());
        ticketItemRepository.save(ticket);
        return "TIKET_VALID";
    }
}