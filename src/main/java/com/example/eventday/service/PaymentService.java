package com.example.eventday.service;

import com.example.eventday.dto.PaymentResponse;
import com.example.eventday.entity.Order;
import com.example.eventday.entity.Payment;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public PaymentResponse payOrder(UUID orderId, String paymentMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order tidak ditemukan!"));

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Order sudah tidak valid atau kedaluwarsa!");
        }

        if (order.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Order sudah kedaluwarsa!");
        }

        if (paymentRepository.findByOrderOrderId(orderId).isPresent()) {
            throw new RuntimeException("Order ini sudah memiliki pembayaran!");
        }

        order.setStatus(Order.OrderStatus.SUCCESS);
        orderRepository.save(order);

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(paymentMethod)
                .paymentStatus(Payment.PaymentStatus.SUCCESS)
                .transactionIdGateway("TRX-GW-" + UUID.randomUUID().toString().substring(0, 8))
                .paidAt(LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        auditLogService.log(order.getCustomer().getUserId(), order.getCustomer().getName(), "PAYMENT", "ORDER",
                order.getOrderId().toString(), "Pembayaran sukses " + order.getOrderNumber() + " via " + paymentMethod);

        return PaymentResponse.builder()
                .paymentId(savedPayment.getPaymentId())
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .paymentMethod(savedPayment.getPaymentMethod())
                .paymentStatus(savedPayment.getPaymentStatus())
                .transactionIdGateway(savedPayment.getTransactionIdGateway())
                .paidAt(savedPayment.getPaidAt())
                .build();
    }
}
