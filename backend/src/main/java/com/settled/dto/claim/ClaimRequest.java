package com.settled.dto.claim;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ClaimRequest(
        @NotNull(message = "Policy is required")
        UUID policyId,

        @NotNull(message = "Incident date is required")
        @PastOrPresent(message = "Incident date cannot be in the future")
        LocalDate incidentDate,

        @NotBlank(message = "Incident type is required")
        @Size(max = 80)
        String incidentType,

        @NotBlank(message = "Description is required")
        @Size(max = 2000)
        String description,

        @NotNull(message = "Amount requested is required")
        @DecimalMin(value = "0.01", message = "Amount requested must be positive")
        BigDecimal amountRequested
) {
}