package com.example.eventday.service;

import com.example.eventday.dto.RescheduleRequestDto;
import com.example.eventday.entity.Event;
import com.example.eventday.entity.RescheduleRequest;
import com.example.eventday.repository.EventRepository;
import com.example.eventday.repository.RescheduleRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RescheduleService {

    private final RescheduleRequestRepository rescheduleRepository;
    private final EventRepository eventRepository;
    private final AuditLogService auditLogService;

    // EO Mengajukan Reschedule
    @Transactional
    public RescheduleRequest createRescheduleRequest(RescheduleRequestDto dto) {
        Event event = eventRepository.findById(dto.getEventId())
                .orElseThrow(() -> new RuntimeException("Event tidak ditemukan!"));

        validateDates(dto.getNewStartDate(), dto.getNewEndDate());

        RescheduleRequest request = RescheduleRequest.builder()
                .event(event)
                .newStartDate(dto.getNewStartDate())
                .newEndDate(dto.getNewEndDate())
                .reason(dto.getReason())
                .status(RescheduleRequest.RescheduleStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        RescheduleRequest saved = rescheduleRepository.save(request);
        auditLogService.log(event.getOrganizer().getUserId(), event.getOrganizer().getName(),
                "CREATE", "RescheduleRequest", saved.getRescheduleId().toString(),
                "Reschedule event " + event.getTitle());
        return saved;
    }

    // Super Admin Menyutujui (ACC) Reschedule
    @Transactional
    public RescheduleRequest approveReschedule(UUID rescheduleId) {
        RescheduleRequest request = rescheduleRepository.findById(rescheduleId)
                .orElseThrow(() -> new RuntimeException("Pengajuan reschedule tidak ditemukan!"));

        validateDates(request.getNewStartDate(), request.getNewEndDate());

        request.setStatus(RescheduleRequest.RescheduleStatus.APPROVED);

        Event event = request.getEvent();
        event.setStartDate(request.getNewStartDate());
        event.setEndDate(request.getNewEndDate());
        eventRepository.save(event);

        RescheduleRequest saved = rescheduleRepository.save(request);
        auditLogService.log(null, "Super Admin", "APPROVE", "RescheduleRequest",
                saved.getRescheduleId().toString(), "Reschedule event " + event.getTitle() + " disetujui");
        return saved;
    }

    @Transactional
    public RescheduleRequest rejectReschedule(UUID rescheduleId) {
        RescheduleRequest request = rescheduleRepository.findById(rescheduleId)
                .orElseThrow(() -> new RuntimeException("Pengajuan reschedule tidak ditemukan!"));

        request.setStatus(RescheduleRequest.RescheduleStatus.REJECTED);

        RescheduleRequest saved = rescheduleRepository.save(request);
        auditLogService.log(null, "Super Admin", "REJECT", "RescheduleRequest",
                saved.getRescheduleId().toString(), "Reschedule event " + saved.getEvent().getTitle() + " ditolak");
        return saved;
    }

    public List<RescheduleRequest> getAllRequests() {
        return rescheduleRepository.findAll();
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new RuntimeException("Tanggal baru tidak boleh kosong!");
        }
        if (!end.isAfter(start)) {
            throw new RuntimeException("Tanggal selesai harus setelah tanggal mulai!");
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Jadwal baru tidak boleh di masa lampau!");
        }
    }
}