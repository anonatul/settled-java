package com.settled.dto.claim;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClaimRejectRequest(
        @NotBlank(message = "Rejection reason is required")
        @Size(max = 2000)
        String reason
) {
}