package com.settled.dto.claim;

import com.settled.domain.ClaimStatusHistory;
import com.settled.domain.enums.ClaimStatus;

import java.time.Instant;
import java.util.UUID;

public record StatusHistoryResponse(
        UUID id,
        ClaimStatus fromStatus,
        ClaimStatus toStatus,
        String changedByName,
        String note,
        Instant changedAt
) {
    public static StatusHistoryResponse from(ClaimStatusHistory history) {
        return new StatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy().getFullName(),
                history.getNote(),
                history.getChangedAt()
        );
    }
}