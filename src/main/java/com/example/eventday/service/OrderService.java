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

            // Cek apakah user sudah memiliki order aktif (PENDING / WAITING_PAYMENT) yang belum expired untuk tier ini
            Optional<Order> existingOrder = orderRepository
                    .findFirstByCustomerUserIdAndTicketTierTierIdAndStatusInAndExpiredAtAfterOrderByCreatedAtDesc(
                            userId,
                            request.getTierId(),
                            List.of("PENDING", "WAITING_PAYMENT"),
                            LocalDateTime.now()
                    );

            if (existingOrder.isPresent()) {
                log.info("Mengembalikan order aktif yang sudah ada untuk User ID: {} dan Order ID: {}",
                        userId, existingOrder.get().getOrderId());
                return existingOrder.get();
            }
        }

        TicketTier tier = ticketTierRepository.findById(request.getTierId())
                .orElseThrow(() -> new IllegalArgumentException("Ticket tier tidak ditemukan"));

        // [ATOMIC UPDATE & RACE CONDITION GUARD]: 
        // Mengurangi kuota langsung di level database. Jika kuota < request.getQuantity(), 
        // query mengembalikan 0 dan transaksi langsung dibatalkan (mencegah overselling).
        int updated = ticketTierRepository.decrementAvailableQuota(request.getTierId(), request.getQuantity());
        if (updated == 0) {
            throw new IllegalStateException("Kuota tiket tidak mencukupi atau sedang dipesan pengguna lain");
        }

        BigDecimal subtotal = tier.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.10)); // Pajak 10%
        BigDecimal totalAmount = subtotal.add(adminFee).add(tax);

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

    // [ATOMIC RESTORE]: Pengembalian kuota tiket secara atomik saat order EXPIRED atau CANCELLED
    @Transactional
    public void handleExpiredOrCancelledOrder(Order order) {
        if ("EXPIRED".equalsIgnoreCase(order.getStatus()) || "CANCELLED".equalsIgnoreCase(order.getStatus())) {
            log.info("Mengembalikan kuota tiket sebanyak {} untuk Order ID: {}", order.getQuantity(), order.getOrderId());
            if (order.getTicketTier() != null && order.getQuantity() != null) {
                ticketTierRepository.incrementAvailableQuota(order.getTicketTier().getTierId(), order.getQuantity());
            }
        }
    }

    // 1. Simpan Data Peserta ke DB
    @Transactional
    public List<Attendee> saveAttendees(AttendeeRequest request) {
        if (request.getAttendees() == null || request.getAttendees().isEmpty()) {
            return Collections.emptyList();
        }

        if (request.getOrderId() != null && !request.getOrderId().isBlank()) {
            try {
                UUID orderUuid = UUID.fromString(request.getOrderId());
                if (!orderRepository.existsById(orderUuid)) {
                    throw new IllegalArgumentException("Order ID tidak valid atau tidak ditemukan");
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Format UUID Order ID tidak valid");
            }
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

    // 4. Cek Status Order
    public String getOrderStatus(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan"));
        return order.getStatus();
    }
}