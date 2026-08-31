package com.example.eventday.service;

import com.example.eventday.entity.Order;
import com.example.eventday.entity.Order.OrderStatus;
import com.example.eventday.entity.TicketItem;
import com.example.eventday.entity.TicketTier;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.repository.TicketItemRepository;
import com.example.eventday.repository.TicketTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderScheduler {

    private final OrderRepository orderRepository;
    private final TicketItemRepository ticketItemRepository;
    private final TicketTierRepository ticketTierRepository;

    // Menjalankan pengecekan setiap 60 detik (1 menit)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredOrders() {
        List<Order> expiredOrders = orderRepository.findByStatusAndExpiredAtBefore(
                OrderStatus.PENDING, LocalDateTime.now()
        );

        for (Order order : expiredOrders) {
            // 1. Ubah status Order menjadi EXPIRED
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);

            // 2. Kembalikan kuota ke masing-masing TicketTier yang batal dipesan
            List<TicketItem> items = ticketItemRepository.findByOrderOrderId(order.getOrderId());
            for (TicketItem item : items) {
                TicketTier tier = item.getTier();
                tier.setAvailableQuota(tier.getAvailableQuota() + 1);
                ticketTierRepository.save(tier);
            }
        }
    }
}