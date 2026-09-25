package com.example.eventday.service.organizer;

import com.example.eventday.repository.OrderRepository;
import com.example.eventday.repository.OrganizerPayoutRepository;
import com.example.eventday.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizerBalanceService {

    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;
    private final OrganizerPayoutRepository organizerPayoutRepository;

    @Transactional(readOnly = true)
    public BigDecimal calculateGrossRevenue(UUID organizerId) {
        BigDecimal v = orderRepository.sumPaidRevenueByOrganizer(organizerId);
        return v != null ? v : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalRefund(UUID organizerId) {
        BigDecimal v = refundRepository.sumApprovedRefundByOrganizer(organizerId);
        return v != null ? v : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalPayout(UUID organizerId) {
        BigDecimal legacy = refundRepository.sumApprovedPayoutLegacyByOrganizer(organizerId);
        BigDecimal current = organizerPayoutRepository.sumApprovedPayoutByOrganizer(organizerId);
        BigDecimal l = legacy != null ? legacy : BigDecimal.ZERO;
        BigDecimal c = current != null ? current : BigDecimal.ZERO;
        return l.add(c);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateAvailableBalance(UUID organizerId) {
        BigDecimal gross = calculateGrossRevenue(organizerId);
        BigDecimal refund = calculateTotalRefund(organizerId);
        BigDecimal payout = calculateTotalPayout(organizerId);
        BigDecimal net = gross.subtract(refund).subtract(payout);
        if (net.compareTo(BigDecimal.ZERO) < 0) net = BigDecimal.ZERO;
        return net;
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> calculateBalanceBreakdown(UUID organizerId) {
        BigDecimal gross = calculateGrossRevenue(organizerId);
        BigDecimal refund = calculateTotalRefund(organizerId);
        BigDecimal payout = calculateTotalPayout(organizerId);
        BigDecimal net = gross.subtract(refund).subtract(payout);
        if (net.compareTo(BigDecimal.ZERO) < 0) net = BigDecimal.ZERO;
        Map<String, BigDecimal> m = new HashMap<>();
        m.put("gross_revenue", gross);
        m.put("total_refund", refund);
        m.put("total_payout", payout);
        m.put("available_balance", net);
        m.put("net_balance", net);
        return m;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculatePendingPayout(UUID organizerId) {
        BigDecimal v = organizerPayoutRepository.sumPendingPayoutByOrganizer(organizerId);
        return v != null ? v : BigDecimal.ZERO;
    }
}
