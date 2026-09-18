package com.example.eventday.service;

import com.example.eventday.dto.*;
import com.example.eventday.entity.Order;
import com.example.eventday.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PaymentService {

    private final OrderRepository orderRepository;
    private final TicketService ticketService;

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

        if (!"PENDING".equalsIgnoreCase(order.getStatus()) && !"WAITING_PAYMENT".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Order tidak dapat di-charge pada status " + order.getStatus());
        }

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

    /**
     * Memproses callback/webhook notifikasi pembayaran dari Midtrans.
     * Mengubah status order menjadi PAID dan otomatis menerbitkan tiket ke database.
     */
    @Transactional
    public void processMidtransNotification(Map<String, Object> payload) {
        String orderIdStr = (String) payload.get("order_id");
        String transactionStatus = (String) payload.get("transaction_status");
        String fraudStatus = (String) payload.get("fraud_status");

        if (orderIdStr == null) {
            throw new IllegalArgumentException("Payload webhook tidak valid: order_id kosong");
        }

        if (orderIdStr.startsWith("ORD-")) {
            orderIdStr = orderIdStr.replace("ORD-", "");
        }
        UUID orderId = UUID.fromString(orderIdStr);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan dengan ID: " + orderId));

        boolean isSuccess = "settlement".equals(transactionStatus)
                || ("capture".equals(transactionStatus) && "accept".equals(fraudStatus));

        if (isSuccess) {
            if (!"PAID".equals(order.getStatus())) {
                order.setStatus("PAID");
                order.setPaidAt(LocalDateTime.now());
                orderRepository.save(order);

                // Menerbitkan record tiket ke database ticket_items
                ticketService.generateTicketsForOrder(order);
            }
        } else if ("cancel".equals(transactionStatus) || "expire".equals(transactionStatus) || "deny".equals(transactionStatus)) {
            order.setStatus("CANCELLED");
            orderRepository.save(order);
        }
    }
}