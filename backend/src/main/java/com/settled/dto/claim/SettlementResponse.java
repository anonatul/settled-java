package com.settled.dto.claim;

import com.settled.domain.Settlement;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SettlementResponse(
        UUID id,
        UUID claimId,
        String settlementNumber,
        BigDecimal approvedAmount,
        BigDecimal settledAmount,
        LocalDate settlementDate,
        String paymentReference,
        String processedByName,
        Instant createdAt
) {
    public static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(
                settlement.getId(),
                settlement.getClaim().getId(),
                settlement.getSettlementNumber(),
                settlement.getApprovedAmount(),
                settlement.getSettledAmount(),
                settlement.getSettlementDate(),
                settlement.getPaymentReference(),
                settlement.getProcessedBy().getFullName(),
                settlement.getCreatedAt()
        );
    }
}