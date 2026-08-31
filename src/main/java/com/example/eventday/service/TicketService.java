package com.example.eventday.service;

import com.example.eventday.dto.TicketResponse;
import com.example.eventday.entity.Order;
import com.example.eventday.entity.TicketItem;
import com.example.eventday.repository.TicketItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketItemRepository ticketItemRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public TicketResponse scanTicket(UUID ticketItemId) {
        TicketItem ticket = ticketItemRepository.findById(ticketItemId)
                .orElseThrow(() -> new RuntimeException("Tiket tidak valid / Tidak ditemukan!"));

        if (ticket.getOrder().getStatus() != Order.OrderStatus.SUCCESS) {
            throw new RuntimeException("Tiket belum lunas / Pembayaran gagal!");
        }

        if (ticket.getCheckInStatus() == TicketItem.CheckInStatus.REDEEMED) {
            throw new RuntimeException("Gagal: Tiket sudah pernah di-scan pada " + ticket.getCheckInAt());
        }

        ticket.setCheckInStatus(TicketItem.CheckInStatus.REDEEMED);
        ticket.setCheckInAt(LocalDateTime.now());

        TicketItem savedTicket = ticketItemRepository.save(ticket);

        auditLogService.log(null, "Scanner", "CHECK_IN", "TICKET",
                savedTicket.getTicketItemId().toString(),
                "Check-in " + savedTicket.getTicketCode() + " atas nama " + savedTicket.getAttendeeName());

        return TicketResponse.builder()
                .ticketItemId(savedTicket.getTicketItemId())
                .orderId(savedTicket.getOrder().getOrderId())
                .orderNumber(savedTicket.getOrder().getOrderNumber())
                .tierId(savedTicket.getTier().getTierId())
                .tierName(savedTicket.getTier().getTierName())
                .ticketCode(savedTicket.getTicketCode())
                .attendeeName(savedTicket.getAttendeeName())
                .attendeeNik(savedTicket.getAttendeeNik())
                .checkInStatus(savedTicket.getCheckInStatus())
                .checkInAt(savedTicket.getCheckInAt())
                .build();
    }
}
