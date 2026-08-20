package com.settled.dto.claim;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ClaimAssignRequest(
        @NotNull(message = "Officer is required")
        UUID officerId,

        @Size(max = 2000)
        String note
) {
}