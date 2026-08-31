package com.example.eventday.service;

import com.example.eventday.dto.CreateOrderRequest;
import com.example.eventday.entity.*;
import com.example.eventday.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketTierRepository ticketTierRepository;
    private final TicketItemRepository ticketItemRepository;
    private final SettingsService settingsService;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        User customer = userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer tidak ditemukan!"));

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event tidak ditemukan!"));

        TicketTier tier = ticketTierRepository.findById(request.getTierId())
                .orElseThrow(() -> new RuntimeException("Tier tidak ditemukan!"));

        int ticketQuantity = request.getAttendees().size();

        if (ticketQuantity > tier.getMaxPerUser()) {
            throw new RuntimeException("Melebihi batas maksimal pembelian tiket per user!");
        }

        if (tier.getAvailableQuota() < ticketQuantity) {
            throw new RuntimeException("Kuota tiket tidak mencukupi!");
        }

        // 1. Kurangi Kuota Tiket
        tier.setAvailableQuota(tier.getAvailableQuota() - ticketQuantity);
        ticketTierRepository.save(tier);

        // 2. Buat Transaksi Order
        BigDecimal totalAmount = tier.getPrice().multiply(BigDecimal.valueOf(ticketQuantity));
        BigDecimal adminFee = settingsService.getAdminFee();

        Order order = Order.builder()
                .orderNumber("ORD-" + System.currentTimeMillis())
                .customer(customer)
                .event(event)
                .totalAmount(totalAmount.add(adminFee))
                .adminFee(adminFee)
                .status(Order.OrderStatus.PENDING)
                .expiredAt(LocalDateTime.now().plusMinutes(settingsService.getExpiryMinutes()))
                .build();

        Order savedOrder = orderRepository.save(order);

        // 3. Generate Item Tiket untuk Setiap Pengunjung
        for (CreateOrderRequest.AttendeeRequest attendee : request.getAttendees()) {
            TicketItem item = TicketItem.builder()
                    .order(savedOrder)
                    .tier(tier)
                    .ticketCode("TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .attendeeName(attendee.getName())
                    .attendeeNik(attendee.getNik())
                    .checkInStatus(TicketItem.CheckInStatus.UNREDEEMED)
                    .build();
            ticketItemRepository.save(item);
        }

        return savedOrder;
    }
}