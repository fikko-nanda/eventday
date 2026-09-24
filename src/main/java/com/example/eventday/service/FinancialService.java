package com.example.eventday.service;

import com.example.eventday.dto.PayoutRequestDto;
import com.example.eventday.entity.EOPayoutBalance;
import com.example.eventday.entity.OrganizerPayout;
import com.example.eventday.repository.EOPayoutBalanceRepository;
import com.example.eventday.repository.OrganizerPayoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FinancialService {

    private final EOPayoutBalanceRepository balanceRepository;
    private final OrganizerPayoutRepository payoutRepository;

    public FinancialService(EOPayoutBalanceRepository balanceRepository, OrganizerPayoutRepository payoutRepository) {
        this.balanceRepository = balanceRepository;
        this.payoutRepository = payoutRepository;
    }

    public EOPayoutBalance getOrganizerBalance(UUID organizerId, UUID eventId) {
        return balanceRepository.findByOrganizerIdAndEventId(organizerId, eventId)
                .orElseThrow(() -> new RuntimeException("Saldo tidak ditemukan untuk event ini"));
    }

    @Transactional
    public OrganizerPayout requestPayout(PayoutRequestDto request) {
        EOPayoutBalance balance = getOrganizerBalance(request.getOrganizerId(), request.getEventId());

        if (balance.getNetBalance() == null || balance.getNetBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Saldo tidak mencukupi untuk penarikan dana ini");
        }

        OrganizerPayout payout = new OrganizerPayout();
        payout.setAmount(request.getAmount());
        payout.setBankName(request.getBankName());
        payout.setBankAccountNumber(request.getBankAccountNumber());
        payout.setAccountHolder(request.getAccountHolder());
        payout.setStatus("PENDING");

        return payoutRepository.save(payout);
    }
}