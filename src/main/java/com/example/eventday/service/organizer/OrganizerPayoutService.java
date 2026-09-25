package com.example.eventday.service.organizer;

import com.example.eventday.entity.Organizer;
import com.example.eventday.entity.OrganizerPayout;
import com.example.eventday.repository.EventRepository;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.repository.OrganizerPayoutRepository;
import com.example.eventday.repository.OrganizerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class OrganizerPayoutService {

    private final OrganizerHelperService helperService;
    private final OrganizerRepository organizerRepository;
    private final OrderRepository orderRepository;
    private final OrganizerPayoutRepository organizerPayoutRepository;
    private final EventRepository eventRepository;
    private final OrganizerBalanceService balanceService;

    public List<Map<String, Object>> getBankAccounts() {
        UUID uid = helperService.currentUserId();
        if (uid != null) {
            Optional<Organizer> opt = organizerRepository.findByUserUserId(uid);
            if (opt.isPresent() && opt.get().getBankName() != null) {
                Organizer o = opt.get();
                Map<String, Object> bank = new HashMap<>();
                bank.put("id", o.getOrganizerId().toString());
                bank.put("bank_name", o.getBankName());
                bank.put("account_number", o.getBankAccountNumber());
                bank.put("account_holder", o.getNameOrganizer());
                bank.put("is_primary", true);
                return List.of(bank);
            }
        }
        Map<String, Object> bank = new HashMap<>();
        bank.put("id", 1);
        bank.put("bank_name", "BCA");
        bank.put("account_number", "8830123456");
        bank.put("account_holder", "PT Harmoni Musik Indonesia");
        bank.put("is_primary", true);
        bank.put("mock", true);
        return List.of(bank);
    }

    public Map<String, Object> getPayoutBalance(UUID eventId) {
        UUID uid = helperService.currentUserId();
        if (uid != null) {
            Optional<Organizer> orgOpt = organizerRepository.findByUserUserId(uid);
            if (orgOpt.isPresent()) {
                UUID organizerId = orgOpt.get().getOrganizerId();
                Map<String, BigDecimal> breakdown = balanceService.calculateBalanceBreakdown(organizerId);
                BigDecimal gross = breakdown.get("gross_revenue");
                BigDecimal available = breakdown.get("available_balance");
                BigDecimal pending = balanceService.calculatePendingPayout(organizerId);
                Map<String, Object> balance = new HashMap<>();
                balance.put("event_id", eventId);
                balance.put("organizer_id", organizerId.toString());
                balance.put("total_sales", gross);
                balance.put("gross_revenue", gross);
                balance.put("total_refund", breakdown.get("total_refund"));
                balance.put("total_payout", breakdown.get("total_payout"));
                balance.put("withdrawable_balance", available);
                balance.put("available_balance", available);
                balance.put("pending_payout", pending);
                balance.put("currency", "IDR");
                return balance;
            }
        }
        Map<String, Object> balance = new HashMap<>();
        balance.put("event_id", eventId);
        balance.put("total_sales", BigDecimal.ZERO);
        balance.put("gross_revenue", BigDecimal.ZERO);
        balance.put("withdrawable_balance", BigDecimal.ZERO);
        balance.put("available_balance", BigDecimal.ZERO);
        balance.put("pending_payout", BigDecimal.ZERO);
        balance.put("currency", "IDR");
        balance.put("mock", true);
        return balance;
    }

    public List<Map<String, Object>> getPayouts() {
        UUID uid = helperService.currentUserId();
        if (uid != null) {
            Optional<Organizer> orgOpt = organizerRepository.findByUserUserId(uid);
            if (orgOpt.isPresent()) {
                List<OrganizerPayout> list =
                        organizerPayoutRepository.findByOrganizerOrganizerId(orgOpt.get().getOrganizerId());
                if (!list.isEmpty()) {
                    return list.stream().map(p -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", p.getPayoutId().toString());
                        m.put("payout_id", p.getPayoutId().toString());
                        m.put("amount", p.getAmount());
                        m.put("status", p.getStatus());
                        m.put("bank_name", p.getBankName());
                        m.put("account_number", p.getBankAccountNumber());
                        m.put("account_holder", p.getAccountHolder());
                        m.put("requested_at", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
                        m.put("processed_at", p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null);
                        return m;
                    }).collect(Collectors.toList());
                }
            }
        }
        Map<String, Object> payout = new HashMap<>();
        payout.put("id", 101);
        payout.put("amount", 25000000);
        payout.put("status", "SUCCESS");
        payout.put("requested_at", "2026-09-10");
        payout.put("mock", true);
        return List.of(payout);
    }

    @Transactional
    public Map<String, Object> createPayout(Map<String, Object> request) {
        UUID uid = helperService.currentUserId();
        Organizer organizer = null;
        if (uid != null) {
            organizer = organizerRepository.findByUserUserId(uid).orElse(null);
        }

        if (organizer != null && request.containsKey("amount")) {
            BigDecimal amount;
            try {
                amount = new BigDecimal(String.valueOf(request.get("amount")));
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nominal payout tidak valid");
            }
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nominal payout harus lebih dari 0");
            }
            BigDecimal available = balanceService.calculateAvailableBalance(organizer.getOrganizerId());
            if (amount.compareTo(available) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Nominal melebihi saldo tersedia (" + available + ")");
            }
            try {
                OrganizerPayout entity = new OrganizerPayout();
                entity.setOrganizer(organizer);

                Object eventIdObj = request.getOrDefault("eventId", request.get("event_id"));
                if (eventIdObj != null) {
                    try {
                        UUID eventId = UUID.fromString(String.valueOf(eventIdObj));
                        eventRepository.findById(eventId).ifPresent(entity::setEvent);
                    } catch (IllegalArgumentException ignored) {}
                }

                entity.setAmount(amount);
                entity.setBankName((String) request.getOrDefault("bank_name", request.getOrDefault("bankName", "BCA")));
                entity.setBankAccountNumber((String) request.getOrDefault("account_number", request.getOrDefault("accountNumber", "")));
                entity.setAccountHolder((String) request.getOrDefault("account_holder", request.getOrDefault("accountHolderName", "")));
                entity.setStatus("PENDING_APPROVAL");

                organizerPayoutRepository.save(entity);

                Map<String, Object> payout = new HashMap<>();
                payout.put("payout_id", entity.getPayoutId().toString());
                payout.put("amount", amount);
                payout.put("status", entity.getStatus());
                payout.put("organizer_id", organizer.getOrganizerId().toString());
                return payout;
            } catch (ResponseStatusException e) {
                throw e;
            } catch (Exception e) {
                log.warn("createPayout DB fail: {}", e.getMessage());
            }
        }

        Map<String, Object> payout = new HashMap<>();
        payout.put("payout_id", 102);
        payout.put("amount", request.getOrDefault("amount", 10000000));
        payout.put("status", "PENDING_APPROVAL");
        payout.put("mock", true);
        return payout;
    }

    public Map<String, Object> getPayoutDetailByString(String id) {
        Organizer org = helperService.resolveCurrentOrganizer();
        UUID uid;
        try {
            uid = UUID.fromString(id);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format ID payout tidak valid");
        }

        OrganizerPayout p = organizerPayoutRepository.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Data payout tidak ditemukan"));

        if (p.getOrganizer() == null || !p.getOrganizer().getOrganizerId().equals(org.getOrganizerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Akses ditolak: Data bukan milik Anda");
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("id", p.getPayoutId().toString());
        detail.put("amount", p.getAmount());
        detail.put("status", p.getStatus());
        detail.put("bank_name", p.getBankName());
        detail.put("account_number", p.getBankAccountNumber());
        detail.put("account_holder", p.getAccountHolder());
        detail.put("created_at", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        return detail;
    }
}