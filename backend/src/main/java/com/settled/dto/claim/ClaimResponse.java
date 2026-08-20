package com.settled.dto.claim;

import com.settled.domain.Claim;
import com.settled.domain.ClaimAssignment;
import com.settled.domain.Settlement;
import com.settled.domain.enums.ClaimStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClaimResponse(
        UUID id,
        String claimNumber,
        ClaimStatus status,
        LocalDate incidentDate,
        String incidentType,
        String description,
        BigDecimal amountRequested,
        BigDecimal amountApproved,
        Instant submittedAt,
        Instant decidedAt,
        Instant settledAt,
        UUID customerId,
        String customerNumber,
        String customerName,
        UUID policyId,
        String policyNumber,
        String policyTypeName,
        BigDecimal policySumInsured,
        UUID assignedOfficerId,
        String assignedOfficerName,
        UUID settlementId,
        String settlementNumber,
        BigDecimal settledAmount,
        Instant createdAt
) {
    public static ClaimResponse from(Claim claim, ClaimAssignment assignment, Settlement settlement) {
        return new ClaimResponse(
                claim.getId(),
                claim.getClaimNumber(),
                claim.getStatus(),
                claim.getIncidentDate(),
                claim.getIncidentType(),
                claim.getDescription(),
                claim.getAmountRequested(),
                claim.getAmountApproved(),
                claim.getSubmittedAt(),
                claim.getDecidedAt(),
                claim.getSettledAt(),
                claim.getCustomer().getId(),
                claim.getCustomer().getCustomerNumber(),
                claim.getCustomer().getUser().getFullName(),
                claim.getPolicy().getId(),
                claim.getPolicy().getPolicyNumber(),
                claim.getPolicy().getPolicyType().getName(),
                claim.getPolicy().getSumInsured(),
                assignment != null && assignment.getOfficer() != null ? assignment.getOfficer().getId() : null,
                assignment != null && assignment.getOfficer() != null ? assignment.getOfficer().getFullName() : null,
                settlement != null ? settlement.getId() : null,
                settlement != null ? settlement.getSettlementNumber() : null,
                settlement != null ? settlement.getSettledAmount() : null,
                claim.getCreatedAt()
        );
    }
}