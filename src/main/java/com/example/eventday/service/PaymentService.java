package com.example.eventday.service;

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

    @Transactional
    public Payment payOrder(UUID orderId, String paymentMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order tidak ditemukan!"));

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Order sudah tidak valid atau kedaluwarsa!");
        }

        // Ubah Status Order menjadi SUCCESS
        order.setStatus(Order.OrderStatus.SUCCESS);
        orderRepository.save(order);

        // Catat Pembayaran
        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(paymentMethod)
                .paymentStatus(Payment.PaymentStatus.SUCCESS)
                .transactionIdGateway("TRX-GW-" + UUID.randomUUID().toString().substring(0, 8))
                .paidAt(LocalDateTime.now())
                .build();

        return paymentRepository.save(payment);
    }
}