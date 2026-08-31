package com.example.eventday.service;

import com.example.eventday.dto.CreateEventRequest;
import com.example.eventday.dto.EventResponse;
import com.example.eventday.entity.Event;
import com.example.eventday.entity.User;
import com.example.eventday.repository.EventRepository;
import com.example.eventday.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventResponse createEvent(CreateEventRequest request) {
        User organizer = userRepository.findById(request.getOrganizerId())
                .orElseThrow(() -> new RuntimeException("Organizer tidak ditemukan!"));

   
            String facilityString = (request.getFacility() != null && !request.getFacility().isEmpty())
                    ? String.join(", ", request.getFacility())
                    : null;

            Event event = Event.builder()
                    .organizer(organizer)
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .category(request.getCategory())
                    .venueName(request.getVenueName())
                    .bannerUrl(request.getBannerUrl())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .facility(facilityString) // Disimpan sebagai String: "WiFi, AC, VIP Parking"
                    .status(Event.EventStatus.PUBLISHED)
                    .build();

        Event savedEvent = eventRepository.save(event);
        return mapToResponse(savedEvent);
    }

    public List<EventResponse> getAllPublishedEvents() {
        return eventRepository.findByStatus(Event.EventStatus.PUBLISHED)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public EventResponse getEventById(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event tidak ditemukan!"));
        return mapToResponse(event);
    }

    private EventResponse mapToResponse(Event event) {
        return EventResponse.builder()
                .eventId(event.getEventId())
                .organizerId(event.getOrganizer().getUserId())
                .organizerName(event.getOrganizer().getName())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategory())
                .venueName(event.getVenueName())
                .bannerUrl(event.getBannerUrl())
                .facility(event.getFacility())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .status(event.getStatus())
                .build();
    }
}