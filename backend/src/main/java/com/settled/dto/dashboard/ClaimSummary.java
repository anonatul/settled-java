package com.settled.dto.dashboard;

import com.settled.domain.Claim;
import com.settled.domain.enums.ClaimStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ClaimSummary(
        UUID id,
        String claimNumber,
        ClaimStatus status,
        String incidentType,
        BigDecimal amountRequested,
        Instant submittedAt
) {
    public static ClaimSummary from(Claim claim) {
        return new ClaimSummary(
                claim.getId(),
                claim.getClaimNumber(),
                claim.getStatus(),
                claim.getIncidentType(),
                claim.getAmountRequested(),
                claim.getSubmittedAt()
        );
    }
}