package com.example.eventday.service.organizer;

import com.example.eventday.entity.EOPayoutBalance;
import com.example.eventday.entity.Order;
import com.example.eventday.event.OrderCreatedEvent;
import com.example.eventday.repository.EOPayoutBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesBalanceService {

    private final EOPayoutBalanceRepository balanceRepository;

    @EventListener
    @Transactional
    public void onPaymentSuccess(OrderCreatedEvent event) {
        Order order = event.getOrder();
        if (order == null || order.getEvent() == null || order.getEvent().getOrganizer() == null) {
            log.warn("Gagal memperbarui saldo: Data event atau organizer pada order tidak lengkap.");
            return;
        }

        UUID organizerId = order.getEvent().getOrganizer().getOrganizerId();
        UUID eventId = order.getEvent().getEventId();

        EOPayoutBalance balance = balanceRepository.findByOrganizerIdAndEventId(organizerId, eventId)
                .orElseGet(() -> EOPayoutBalance.builder()
                        .organizerId(organizerId)
                        .eventId(eventId)
                        .grossSales(BigDecimal.ZERO)
                        .totalAdminFees(BigDecimal.ZERO)
                        .totalRefunds(BigDecimal.ZERO)
                        .build());

        BigDecimal orderTotal = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal adminFee = order.getAdminFee() != null ? order.getAdminFee() : BigDecimal.ZERO;

        balance.setGrossSales(balance.getGrossSales().add(orderTotal));
        balance.setTotalAdminFees(balance.getTotalAdminFees().add(adminFee));

        balanceRepository.save(balance);
        log.info("Saldo EO berhasil diperbarui untuk Event ID: {}. Gross Sales +{}", eventId, orderTotal);
    }
}