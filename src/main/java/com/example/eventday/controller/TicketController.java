package com.example.eventday.controller;

import com.example.eventday.dto.TicketResponse;
import com.example.eventday.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/scan/{ticketItemId}")
    public ResponseEntity<?> scanTicket(@PathVariable UUID ticketItemId) {
        try {
            TicketResponse response = ticketService.scanTicket(ticketItemId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
