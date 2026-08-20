package com.settled.dto.claim;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClaimInfoRequest(
        @NotBlank(message = "Request note is required")
        @Size(max = 2000)
        String note
) {
}