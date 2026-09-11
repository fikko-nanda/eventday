package com.example.eventday.service;

import com.example.eventday.dto.InitiateCheckoutRequest;
import com.example.eventday.entity.Order;
import com.example.eventday.entity.TicketTier;
import com.example.eventday.entity.User;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.repository.TicketTierRepository;
import com.example.eventday.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final TicketTierRepository ticketTierRepository;

    @Value("${app.order.admin-fee:5000}")
    private BigDecimal adminFee;

    @Value("${app.order.expiry-minutes:15}")
    private int expiryMinutes;

    @Transactional
    public Order createOrder(UUID userId, InitiateCheckoutRequest request) {
        User customer = null;
        if (userId != null) {
            customer = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
        }

        TicketTier tier = ticketTierRepository.findById(request.getTierId())
                .orElseThrow(() -> new IllegalArgumentException("Ticket tier tidak ditemukan"));

        if (tier.getAvailableQuota() < request.getQuantity()) {
            throw new IllegalStateException("Kuota tiket tidak mencukupi");
        }

        BigDecimal subtotal = tier.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        BigDecimal totalAmount = subtotal.add(adminFee);

        Order order = Order.builder()
                .customer(customer)
                .event(tier.getEvent())
                .ticketTier(tier)
                .quantity(request.getQuantity())
                .adminFee(adminFee)
                .totalAmount(totalAmount)
                .status("PENDING")
                .expiredAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .build();

        return orderRepository.save(order);
    }
}
