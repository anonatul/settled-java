package com.settled.dto.policy;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PolicyRequest(
        @NotNull(message = "Policy type is required")
        UUID policyTypeId,

        @NotNull(message = "Start date is required")
        @FutureOrPresent(message = "Start date must be today or in the future")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @NotNull(message = "Premium is required")
        @Positive(message = "Premium must be positive")
        BigDecimal premium,

        @NotNull(message = "Sum insured is required")
        @Positive(message = "Sum insured must be positive")
        BigDecimal sumInsured
) {
}