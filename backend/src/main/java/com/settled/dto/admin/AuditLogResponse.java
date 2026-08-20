package com.settled.dto.admin;

import com.settled.domain.AuditLog;
import com.settled.domain.enums.AuditAction;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorId,
        String actorEmail,
        String actorName,
        AuditAction action,
        String entityType,
        UUID entityId,
        String details,
        String ipAddress,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActor() != null ? log.getActor().getId() : null,
                log.getActor() != null ? log.getActor().getEmail() : "system",
                log.getActor() != null ? log.getActor().getFullName() : "System",
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }
}