package com.settled.dto.claim;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ClaimDecisionRequest(
        @NotNull(message = "Amount approved is required")
        @DecimalMin(value = "0.01", message = "Approved amount must be positive")
        BigDecimal amountApproved,

        @Size(max = 2000)
        String note
) {
}