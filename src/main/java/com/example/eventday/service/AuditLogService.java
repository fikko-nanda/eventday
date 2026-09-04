package com.example.eventday.service;

import com.example.eventday.entity.AuditLog;
import com.example.eventday.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
@SuppressWarnings("null")
    public void log(UUID actorId, String actorName, String action, String detail) {
        AuditLog log = AuditLog.builder()
                .actorId(actorId)
                .actorName(actorName)
                .action(action)
                .detail(detail)
                .build();
        auditLogRepository.save(log);
    }
}
