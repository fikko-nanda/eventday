package com.example.eventday.service;

import com.example.eventday.dto.RefundRequestDto;
import com.example.eventday.entity.Order;
import com.example.eventday.entity.RefundRequest;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.repository.RefundRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class RefundService {

    private final RefundRequestRepository refundRepository;
    private final OrderRepository orderRepository;

    public RefundService(RefundRequestRepository refundRepository, OrderRepository orderRepository) {
        this.refundRepository = refundRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public RefundRequest createRefundRequest(RefundRequestDto dto) {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order tidak ditemukan!"));

        if (order.getStatus() != Order.OrderStatus.SUCCESS) {
            throw new RuntimeException("Hanya order berstatus SUCCESS yang bisa di-refund!");
        }

        RefundRequest refund = RefundRequest.builder()
                .order(order)
                .reason(dto.getReason())
                .bankName(dto.getBankName())
                .bankAccountNumber(dto.getBankAccountNumber())
                .bankAccountName(dto.getBankAccountName())
                .status(RefundRequest.RefundStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        return refundRepository.save(refund);
    }

    public List<RefundRequest> getAllRefunds() {
        return refundRepository.findAll();
    }
}