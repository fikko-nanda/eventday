package com.example.eventday.service;

import com.example.eventday.dto.*;
import com.example.eventday.entity.Order;
import com.example.eventday.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PaymentService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public CheckoutSummaryResponse getCheckoutSummary(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan"));

        BigDecimal pricePerTicket = order.getTicketTier().getPrice();
        BigDecimal subtotal = pricePerTicket.multiply(BigDecimal.valueOf(order.getQuantity()));
        String orderNumber = "ORD-" + order.getOrderId().toString().substring(0, 8).toUpperCase();

        return CheckoutSummaryResponse.builder()
                .orderId(order.getOrderId())
                .orderNumber(orderNumber)
                .eventTitle(order.getEvent().getTitle())
                .ticketTierName(order.getTicketTier().getTierName())
                .quantity(order.getQuantity())
                .pricePerTicket(pricePerTicket)
                .subtotal(subtotal)
                .adminFee(order.getAdminFee())
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(order.getTotalAmount())
                .expiredAt(order.getExpiredAt())
                .build();
    }

    @Transactional
    public PaymentChargeResponse processPaymentCharge(PaymentChargeRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan"));

        // Charge boleh dari PENDING (langsung) atau WAITING_PAYMENT (sesudah checkout/process) —
        // charge ulang me-regenerate VA (mock). Yang ditolak: order yang sudah final/kedaluwarsa.
        if (!"PENDING".equalsIgnoreCase(order.getStatus()) && !"WAITING_PAYMENT".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Order tidak dapat di-charge pada status " + order.getStatus());
        }

        // Mock Virtual Account Generator
        String vaNumber = "88325" + (System.currentTimeMillis() % 1000000000L);
        order.setStatus("WAITING_PAYMENT");
        order.setPaymentMethod(request.getPaymentMethod());
        orderRepository.save(order);

        String orderNumber = "ORD-" + order.getOrderId().toString().substring(0, 8).toUpperCase();

        return PaymentChargeResponse.builder()
                .orderId(order.getOrderId())
                .orderNumber(orderNumber)
                .totalAmount(order.getTotalAmount())
                .paymentMethod(request.getPaymentMethod())
                .bankCode(request.getBankCode())
                .virtualAccountNumber(vaNumber)
                .expiredAt(order.getExpiredAt())
                .build();
    }
}