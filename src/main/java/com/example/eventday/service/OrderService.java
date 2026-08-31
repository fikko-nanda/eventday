package com.example.eventday.service;

import com.example.eventday.dto.CreateOrderRequest;
import com.example.eventday.dto.OrderResponse;
import com.example.eventday.entity.*;
import com.example.eventday.repository.*;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
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
    private final AuditLogService auditLogService;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
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

        tier.setAvailableQuota(tier.getAvailableQuota() - ticketQuantity);

        try {
            ticketTierRepository.save(tier);
            ticketTierRepository.flush();
        } catch (OptimisticLockingFailureException | OptimisticLockException e) {
            throw new RuntimeException("Kuota tiket sudah diubah oleh pengguna lain, silakan coba lagi!");
        }

        BigDecimal totalAmount = tier.getPrice().multiply(BigDecimal.valueOf(ticketQuantity));
        BigDecimal adminFee = settingsService.getAdminFee();

        Order order = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(customer)
                .event(event)
                .totalAmount(totalAmount.add(adminFee))
                .adminFee(adminFee)
                .status(Order.OrderStatus.PENDING)
                .expiredAt(LocalDateTime.now().plusMinutes(settingsService.getExpiryMinutes()))
                .build();

        Order savedOrder = orderRepository.save(order);

        auditLogService.log(customer.getUserId(), customer.getName(), "CREATE", "ORDER",
                savedOrder.getOrderId().toString(), "Buat order " + savedOrder.getOrderNumber());

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

        return OrderResponse.builder()
                .orderId(savedOrder.getOrderId())
                .orderNumber(savedOrder.getOrderNumber())
                .customerName(customer.getName())
                .customerEmail(customer.getEmail())
                .eventId(event.getEventId())
                .eventTitle(event.getTitle())
                .totalAmount(savedOrder.getTotalAmount())
                .adminFee(savedOrder.getAdminFee())
                .status(savedOrder.getStatus())
                .createdAt(savedOrder.getCreatedAt())
                .expiredAt(savedOrder.getExpiredAt())
                .build();
    }
}
