package com.example.eventday.service;

import com.example.eventday.dto.*;
import com.example.eventday.entity.Order;
import com.example.eventday.entity.TicketTier;
import com.example.eventday.entity.User;
import com.example.eventday.model.Attendee;
import com.example.eventday.repository.AttendeeRepository;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.repository.TicketTierRepository;
import com.example.eventday.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final TicketTierRepository ticketTierRepository;
    private final AttendeeRepository attendeeRepository;

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

        // [PERBAIKAN 1]: Potong kuota tiket di database saat order baru dibuat (Cegah Overselling)
        tier.setAvailableQuota(tier.getAvailableQuota() - request.getQuantity());
        ticketTierRepository.save(tier);

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

    // [PERBAIKAN 2]: Method pengembalian kuota tiket saat order EXPIRED atau CANCELLED
    @Transactional
    public void handleExpiredOrCancelledOrder(Order order) {
        if ("EXPIRED".equalsIgnoreCase(order.getStatus()) || "CANCELLED".equalsIgnoreCase(order.getStatus())) {
            log.info("Mengembalikan kuota tiket sebanyak {} untuk Order ID: {}", order.getQuantity(), order.getOrderId());
            TicketTier tier = order.getTicketTier();
            if (tier != null) {
                tier.setAvailableQuota(tier.getAvailableQuota() + order.getQuantity());
                ticketTierRepository.save(tier);
            }
        }
    }

    // 1. Simpan Data Peserta ke DB
    @Transactional
    public List<Attendee> saveAttendees(AttendeeRequest request) {
        if (request.getAttendees() == null || request.getAttendees().isEmpty()) {
            return Collections.emptyList();
        }

        List<Attendee> savedList = new ArrayList<>();
        for (AttendeeRequest.AttendeeItem item : request.getAttendees()) {
            Attendee attendee = new Attendee();
            attendee.setOrderId(request.getOrderId());
            attendee.setFullName(item.getFullName());
            attendee.setEmail(item.getEmail());
            attendee.setPhoneNumber(item.getPhoneNumber());
            attendee.setIdentityNumber(item.getIdentityNumber());
            savedList.add(attendeeRepository.save(attendee));
        }
        return savedList;
    }

    // 2. Kalkulasi Rincian Checkout
    public CalculationResponse calculateCheckout(CalculationRequest request) {
        TicketTier tier = ticketTierRepository.findById(request.getTierId())
                .orElseThrow(() -> new IllegalArgumentException("Ticket tier tidak ditemukan"));

        BigDecimal subtotal = tier.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.10)); // Pajak 10%
        BigDecimal discount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(adminFee).add(tax).subtract(discount);

        return CalculationResponse.builder()
                .subtotal(subtotal)
                .adminFee(adminFee)
                .tax(tax)
                .discount(discount)
                .totalAmount(totalAmount)
                .build();
    }

    // 3. Process Checkout (Kunci status menjadi WAITING_PAYMENT)
    @Transactional
    public Order processCheckout(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan"));
        if (!"PENDING".equalsIgnoreCase(order.getStatus()) && !"WAITING_PAYMENT".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Order tidak dapat diproses pada status " + order.getStatus());
        }
        order.setStatus("WAITING_PAYMENT");
        return orderRepository.save(order);
    }

    // 4. Cek Status Order (Polling Frontend)
    public String getOrderStatus(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan"));
        return order.getStatus();
    }
}