package com.settled.service;

import com.settled.domain.AuditLog;
import com.settled.domain.User;
import com.settled.domain.enums.AuditAction;
import com.settled.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional
    public void log(User actor, AuditAction action, String entityType, UUID entityId,
                    String details, String ipAddress) {
        try {
            AuditLog entry = new AuditLog();
            entry.setActor(actor);
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setDetails(details);
            entry.setIpAddress(ipAddress);
            entry.setCreatedAt(Instant.now());
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to write audit log for action {}: {}", action, e.getMessage());
        }
    }
}