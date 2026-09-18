package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.entity.Order;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.service.EmailService;
import com.example.eventday.service.MidtransService;
import com.example.eventday.service.OrderService;
import com.example.eventday.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/payments", "/api/v1/payments"})
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PaymentController {

    private final MidtransService midtransService;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final TicketService ticketService;
    private final EmailService emailService;

    @Value("${midtrans.server-key}")
    private String serverKey;

    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<?>> processPayment(@RequestBody Map<String, Object> request) {
        log.info("Request /payments/charge masuk dengan body: {}", request);

        try {
            if (request == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.success("Request body tidak boleh kosong", null));
            }

            String orderIdStr = request.get("orderId") != null ? request.get("orderId").toString()
                    : (request.get("order_id") != null ? request.get("order_id").toString() : null);

            if (orderIdStr == null) {
                log.warn("Validation fail: orderId is null");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.success("Request body wajib menyertakan 'orderId'", null));
            }

            UUID orderId = UUID.fromString(orderIdStr);
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan dengan ID: " + orderIdStr));

            String customerName = request.get("customerName") != null ? request.get("customerName").toString()
                    : (request.get("customer_name") != null ? request.get("customer_name").toString() : "");
            String customerEmail = request.get("customerEmail") != null ? request.get("customerEmail").toString() : "";

            Map<String, String> midtransResponse = midtransService.createSnapTransaction(
                    orderIdStr,
                    order.getTotalAmount(),
                    customerName,
                    customerEmail
            );

            return ResponseEntity.ok(ApiResponse.success("Snap Token berhasil dibuat", midtransResponse));

        } catch (Exception e) {
            log.error("Error Midtrans Snap Charge: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.success("Gagal memproses Snap Midtrans: " + e.getMessage(), null));
        }
    }

    @PostMapping({"/midtrans-notification", "/notification"})
    public ResponseEntity<ApiResponse<String>> handleMidtransNotification(@RequestBody Map<String, Object> notification) {
        try {
            String orderIdStr = String.valueOf(notification.get("order_id"));
            String statusCode = String.valueOf(notification.get("status_code"));
            String grossAmount = String.valueOf(notification.get("gross_amount"));
            String signatureKey = String.valueOf(notification.get("signature_key"));
            String transactionStatus = String.valueOf(notification.get("transaction_status"));
            String fraudStatus = String.valueOf(notification.get("fraud_status"));

            log.info("Notifikasi Midtrans diterima untuk Order ID: {} dengan status: {}", orderIdStr, transactionStatus);

            String cleanKey = (serverKey != null) ? serverKey.trim() : "";
            String rawSignature = orderIdStr + statusCode + grossAmount + cleanKey;
            String calculatedSignature = hashSha512(rawSignature);

            if (!calculatedSignature.equalsIgnoreCase(signatureKey)) {
                log.warn("Peringatan Keamanan: Signature Webhook Midtrans tidak valid untuk Order ID: {}", orderIdStr);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.success("Signature Key Midtrans tidak valid", null));
            }

            Order order = orderRepository.findById(UUID.fromString(orderIdStr))
                    .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan untuk ID: " + orderIdStr));

            // Idempotency check: abaikan notifikasi jika order sudah diproses sebelumnya
            if ("PAID".equals(order.getStatus())) {
                log.info("Order ID {} sudah berstatus PAID. Notifikasi duplikat diabaikan.", orderIdStr);
                return ResponseEntity.ok(ApiResponse.success("Order sudah PAID, notifikasi duplikat diabaikan", "OK"));
            }

            if ("CANCELLED".equals(order.getStatus()) || "EXPIRED".equals(order.getStatus())) {
                log.info("Order ID {} sudah berstatus {}. Notifikasi duplikat diabaikan.", orderIdStr, order.getStatus());
                return ResponseEntity.ok(ApiResponse.success("Order sudah " + order.getStatus() + ", notifikasi duplikat diabaikan", "OK"));
            }

            if ("capture".equals(transactionStatus) || "settlement".equals(transactionStatus)) {
                if ("accept".equals(fraudStatus) || "settlement".equals(transactionStatus)) {
                    order.setStatus("PAID");
                    orderRepository.save(order);
                    log.info("Order ID {} resmi PAID", orderIdStr);

                    try {
                        ticketService.generateTicketsForOrder(order);
                        log.info("Tiket berhasil diterbitkan untuk Order ID: {}", orderIdStr);

                        // Kirim email konfirmasi dan E-Ticket secara async
                        String email = (order.getCustomer() != null && order.getCustomer().getEmail() != null)
                                ? order.getCustomer().getEmail() : "";
                        String eventTitle = (order.getEvent() != null && order.getEvent().getTitle() != null)
                                ? order.getEvent().getTitle() : "Eventday Ticket";

                        emailService.sendOrderConfirmationEmail(email, orderIdStr, eventTitle, order.getQuantity());
                    } catch (Exception e) {
                        log.error("Gagal menerbitkan tiket atau mengirim email untuk Order ID {}: ", orderIdStr, e);
                    }
                }
            } else if ("cancel".equals(transactionStatus) || "deny".equals(transactionStatus) || "expire".equals(transactionStatus)) {
                order.setStatus("EXPIRED".equals(transactionStatus) ? "EXPIRED" : "CANCELLED");
                orderService.handleExpiredOrCancelledOrder(order);
                orderRepository.save(order);
                log.info("Order ID {} dibatalkan/expired. Stok tiket dikembalikan.", orderIdStr);
            }

            return ResponseEntity.ok(ApiResponse.success("Notifikasi Midtrans berhasil diproses", "OK"));

        } catch (Exception e) {
            log.error("Error memproses Notifikasi Midtrans: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.success("Gagal memproses notifikasi: " + e.getMessage(), null));
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