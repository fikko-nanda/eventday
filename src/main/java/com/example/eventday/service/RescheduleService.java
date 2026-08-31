package com.example.eventday.service;

import com.example.eventday.dto.RescheduleRequestDto;
import com.example.eventday.entity.Event;
import com.example.eventday.entity.RescheduleRequest;
import com.example.eventday.repository.EventRepository;
import com.example.eventday.repository.RescheduleRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RescheduleService {

    private final RescheduleRequestRepository rescheduleRepository;
    private final EventRepository eventRepository;

    public RescheduleService(RescheduleRequestRepository rescheduleRepository, EventRepository eventRepository) {
        this.rescheduleRepository = rescheduleRepository;
        this.eventRepository = eventRepository;
    }

    // EO Mengajukan Reschedule
    @Transactional
    public RescheduleRequest createRescheduleRequest(RescheduleRequestDto dto) {
        Event event = eventRepository.findById(dto.getEventId())
                .orElseThrow(() -> new RuntimeException("Event tidak ditemukan!"));

        RescheduleRequest request = RescheduleRequest.builder()
                .event(event)
                .newStartDate(dto.getNewStartDate())
                .newEndDate(dto.getNewEndDate())
                .reason(dto.getReason())
                .status(RescheduleRequest.RescheduleStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        return rescheduleRepository.save(request);
    }

    // Super Admin Menyutujui (ACC) Reschedule
    @Transactional
    public RescheduleRequest approveReschedule(UUID rescheduleId) {
        RescheduleRequest request = rescheduleRepository.findById(rescheduleId)
                .orElseThrow(() -> new RuntimeException("Pengajuan reschedule tidak ditemukan!"));

        request.setStatus(RescheduleRequest.RescheduleStatus.APPROVED);

        // Update Tanggal Resmi di Entitas Event
        Event event = request.getEvent();
        event.setStartDate(request.getNewStartDate());
        event.setEndDate(request.getNewEndDate());
        eventRepository.save(event);

        return rescheduleRepository.save(request);
    }

    public List<RescheduleRequest> getAllRequests() {
        return rescheduleRepository.findAll();
    }
}