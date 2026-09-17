package com.example.eventday.service;

import com.example.eventday.dto.TicketDetailResponse;
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
        // Konversi UUID orderId ke String agar sesuai dengan AttendeeRepository
        String orderIdStr = order.getOrderId() != null ? order.getOrderId().toString() : "";
        List<Attendee> attendees = attendeeRepository.findByOrderId(orderIdStr);
        List<TicketItem> generatedTickets = new ArrayList<>();

        if (attendees != null && !attendees.isEmpty()) {
            for (Attendee attendee : attendees) {
                TicketItem ticket = generateTicket(
                        order,
                        order.getTicketTier(),
                        attendee.getFullName(),
                        attendee.getEmail(),
                        attendee.getIdentityNumber()
                );
                generatedTickets.add(ticket);
            }
        } else {
            // Fallback jika data peserta khusus di tabel Attendee belum diisi
            String email = (order.getCustomer() != null && order.getCustomer().getEmail() != null)
                    ? order.getCustomer().getEmail() : "";

            for (int i = 0; i < order.getQuantity(); i++) {
                TicketItem ticket = generateTicket(
                        order,
                        order.getTicketTier(),
                        "Pemegang Tiket " + (i + 1),
                        email,
                        null
                );
                generatedTickets.add(ticket);
            }
        }
        return generatedTickets;
    }

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
                eventDate = tier.getEvent().getStartDate();
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