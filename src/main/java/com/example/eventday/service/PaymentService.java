package com.example.eventday.service;

import com.example.eventday.dto.*;
import com.example.eventday.entity.Order;
import com.example.eventday.event.OrderCreatedEvent;
import com.example.eventday.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PaymentService {

    private final OrderRepository orderRepository;
    private final TicketService ticketService;
    private final EmailService emailService;
    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher; // Injeksi Publisher Event

    @Value("${midtrans.server-key:}")
    private String serverKey;

    @Transactional(readOnly = true)
    public CheckoutSummaryResponse getCheckoutSummary(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan"));

        BigDecimal pricePerTicket = (order.getTicketTier() != null && order.getTicketTier().getPrice() != null)
                ? order.getTicketTier().getPrice() : BigDecimal.ZERO;
        int quantity = order.getQuantity() != null ? order.getQuantity() : 0;
        BigDecimal subtotal = pricePerTicket.multiply(BigDecimal.valueOf(quantity));
        
        String orderNumber = "ORD-" + order.getOrderId().toString();

        return CheckoutSummaryResponse.builder()
                .orderId(order.getOrderId())
                .orderNumber(orderNumber)
                .eventTitle(order.getEvent() != null ? order.getEvent().getTitle() : "-")
                .ticketTierName(order.getTicketTier() != null ? order.getTicketTier().getTierName() : "-")
                .quantity(quantity)
                .pricePerTicket(pricePerTicket)
                .subtotal(subtotal)
                .adminFee(order.getAdminFee() != null ? order.getAdminFee() : BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO)
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

        String orderNumber = "ORD-" + order.getOrderId().toString();

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

    @Transactional
    public void verifyAndUpdateStatus(String orderIdStr, String transactionId) {
        if (orderIdStr == null || orderIdStr.isBlank()) {
            throw new IllegalArgumentException("Order ID tidak boleh kosong");
        }

        String cleanUUIDStr = orderIdStr.replace("ORD-", "").trim();
        UUID orderId;
        try {
            orderId = UUID.fromString(cleanUUIDStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Order ID tidak valid: " + orderIdStr);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan dengan ID: " + orderIdStr));

        if ("PAID".equalsIgnoreCase(order.getStatus())) {
            log.info("Order ID {} sudah berstatus PAID.", orderIdStr);
            return;
        }

        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        if (transactionId != null && !transactionId.isBlank()) {
            order.setTransactionIdGateway(transactionId);
        }
        orderRepository.save(order);

        // Memicu Event agar saldo EO diperbarui otomatis
        eventPublisher.publishEvent(new OrderCreatedEvent(this, order));

        log.info("Order ID {} berhasil diubah menjadi PAID via verifikasi manual", orderIdStr);

        try {
            ticketService.generateTicketsForOrder(order);
            log.info("Tiket berhasil diterbitkan untuk Order ID: {}", orderIdStr);

            String email = (order.getCustomer() != null && order.getCustomer().getEmail() != null)
                    ? order.getCustomer().getEmail() : "";
            String eventTitle = (order.getEvent() != null && order.getEvent().getTitle() != null)
                    ? order.getEvent().getTitle() : "Eventday Ticket";

            if (!email.isBlank()) {
                emailService.sendOrderConfirmationEmail(email, orderIdStr, eventTitle, order.getQuantity());
            }
        } catch (Exception e) {
            log.error("Gagal menerbitkan tiket atau mengirim email untuk Order ID {}: ", orderIdStr, e);
        }
    }

    @Transactional
    public void processMidtransNotification(Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("order_id")) {
            log.warn("Payload webhook Midtrans kosong atau tidak memiliki order_id");
            return;
        }

        String orderIdStr = String.valueOf(payload.get("order_id"));
        String statusCode = String.valueOf(payload.get("status_code"));
        String grossAmount = String.valueOf(payload.get("gross_amount"));
        String signatureKey = String.valueOf(payload.get("signature_key"));
        String transactionStatus = String.valueOf(payload.get("transaction_status"));
        String fraudStatus = String.valueOf(payload.get("fraud_status"));
        String transactionId = payload.get("transaction_id") != null ? String.valueOf(payload.get("transaction_id")) : null;

        log.info("Notifikasi Midtrans diterima untuk Order ID: {} dengan status: {}", orderIdStr, transactionStatus);

        if ("null".equalsIgnoreCase(orderIdStr) || orderIdStr.isBlank()) {
            log.warn("Payload webhook tidak valid: order_id kosong");
            return;
        }

        // Verifikasi Signature Key Midtrans
        if (signatureKey != null && !"null".equalsIgnoreCase(signatureKey) && serverKey != null && !serverKey.isBlank()) {
            String cleanKey = serverKey.trim();
            String rawSignature = orderIdStr + statusCode + grossAmount + cleanKey;
            String calculatedSignature = hashSha512(rawSignature);

            if (!calculatedSignature.equalsIgnoreCase(signatureKey)) {
                log.warn("Peringatan Keamanan: Signature Webhook Midtrans tidak valid untuk Order ID: {}", orderIdStr);
            }
        }

        String cleanUUIDStr = orderIdStr.replace("ORD-", "").trim();
        UUID orderId;
        try {
            orderId = UUID.fromString(cleanUUIDStr);
        } catch (IllegalArgumentException e) {
            log.warn("Order ID dari Midtrans bukan UUID valid: {}", orderIdStr);
            return;
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Order tidak ditemukan di DB dengan ID: {}. Notifikasi diabaikan.", orderIdStr);
            return;
        }

        if ("PAID".equalsIgnoreCase(order.getStatus())) {
            log.info("Order ID {} sudah berstatus PAID. Notifikasi duplikat diabaikan.", orderIdStr);
            return;
        }

        if ("CANCELLED".equalsIgnoreCase(order.getStatus()) || "EXPIRED".equalsIgnoreCase(order.getStatus())) {
            log.info("Order ID {} sudah berstatus {}. Notifikasi diabaikan.", orderIdStr, order.getStatus());
            return;
        }

        boolean isSuccess = "settlement".equalsIgnoreCase(transactionStatus)
                || ("capture".equalsIgnoreCase(transactionStatus) && "accept".equalsIgnoreCase(fraudStatus));

        if (isSuccess) {
            order.setStatus("PAID");
            order.setPaidAt(LocalDateTime.now());
            if (transactionId != null && !transactionId.isBlank()) {
                order.setTransactionIdGateway(transactionId);
            }
            orderRepository.save(order);

            // Memicu Event agar saldo EO diperbarui otomatis
            eventPublisher.publishEvent(new OrderCreatedEvent(this, order));

            log.info("Order ID {} resmi PAID", orderIdStr);

            try {
                ticketService.generateTicketsForOrder(order);
                log.info("Tiket berhasil diterbitkan untuk Order ID: {}", orderIdStr);

                String email = (order.getCustomer() != null && order.getCustomer().getEmail() != null)
                        ? order.getCustomer().getEmail() : "";
                String eventTitle = (order.getEvent() != null && order.getEvent().getTitle() != null)
                        ? order.getEvent().getTitle() : "Eventday Ticket";

                if (!email.isBlank()) {
                    emailService.sendOrderConfirmationEmail(email, orderIdStr, eventTitle, order.getQuantity());
                }
            } catch (Exception e) {
                log.error("Gagal menerbitkan tiket atau mengirim email untuk Order ID {}: ", orderIdStr, e);
            }
        } else if ("cancel".equalsIgnoreCase(transactionStatus) || "deny".equalsIgnoreCase(transactionStatus) || "expire".equalsIgnoreCase(transactionStatus)) {
            order.setStatus("EXPIRED".equalsIgnoreCase(transactionStatus) ? "EXPIRED" : "CANCELLED");
            orderService.handleExpiredOrCancelledOrder(order);
            orderRepository.save(order);
            log.info("Order ID {} dibatalkan/expired.", orderIdStr);
        }
    }

    private String hashSha512(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder hashtext = new StringBuilder(no.toString(16));
            while (hashtext.length() < 128) {
                hashtext.insert(0, "0");
            }
            return hashtext.toString();
        } catch (Exception e) {
            throw new RuntimeException("Gagal melakukan hashing SHA-512", e);
        }
    }
}