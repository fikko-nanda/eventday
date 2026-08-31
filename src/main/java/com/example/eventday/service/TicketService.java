package com.example.eventday.service;

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

    @Transactional
    public TicketItem scanTicket(UUID ticketItemId) {
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

        return ticketItemRepository.save(ticket);
    }
}