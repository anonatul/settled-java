package com.settled.dto.claim;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SettlementRequest(
        @NotNull(message = "Settled amount is required")
        @DecimalMin(value = "0.01", message = "Settled amount must be positive")
        BigDecimal settledAmount,

        @Size(max = 100)
        String paymentReference,

        @NotNull(message = "Settlement date is required")
        @PastOrPresent(message = "Settlement date cannot be in the future")
        LocalDate settlementDate
) {
}