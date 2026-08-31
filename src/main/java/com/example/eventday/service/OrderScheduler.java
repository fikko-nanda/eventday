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
    private final AuditLogService auditLogService;

    @Scheduled(fixedRate = 60000)
    public void cancelExpiredOrders() {
        List<Order> expiredOrders = orderRepository.findByStatusAndExpiredAtBefore(
                OrderStatus.PENDING, LocalDateTime.now()
        );

        for (Order order : expiredOrders) {
            try {
                processExpiredOrder(order);
            } catch (Exception e) {
                System.err.println("Gagal memproses order " + order.getOrderNumber() + ": " + e.getMessage());
            }
        }
    }

    @Transactional
    public void processExpiredOrder(Order order) {
        order.setStatus(OrderStatus.EXPIRED);
        orderRepository.save(order);

        auditLogService.log(order.getCustomer().getUserId(), order.getCustomer().getName(), "EXPIRE", "ORDER",
                order.getOrderId().toString(), "Order " + order.getOrderNumber() + " otomatis kedaluwarsa");

        List<TicketItem> items = ticketItemRepository.findByOrderOrderId(order.getOrderId());
        for (TicketItem item : items) {
            TicketTier tier = item.getTier();
            tier.setAvailableQuota(tier.getAvailableQuota() + 1);
            ticketTierRepository.save(tier);
        }
    }
}
